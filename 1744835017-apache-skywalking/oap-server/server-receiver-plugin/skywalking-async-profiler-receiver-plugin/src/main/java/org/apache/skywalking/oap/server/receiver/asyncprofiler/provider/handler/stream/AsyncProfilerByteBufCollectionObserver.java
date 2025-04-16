/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.skywalking.oap.server.receiver.asyncprofiler.provider.handler.stream;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.library.jfr.type.Arguments;
import org.apache.skywalking.apm.network.language.asyncprofiler.v10.AsyncProfilerCollectionResponse;
import org.apache.skywalking.apm.network.language.asyncprofiler.v10.AsyncProfilerData;
import org.apache.skywalking.apm.network.language.asyncprofiler.v10.AsyncProfilingStatus;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTask;
import org.apache.skywalking.oap.server.core.query.type.AsyncProfilerTaskLogOperationType;
import org.apache.skywalking.oap.server.core.source.JFRProfilingData;
import org.apache.skywalking.oap.server.core.source.SourceReceiver;
import org.apache.skywalking.oap.server.core.storage.profiling.asyncprofiler.IAsyncProfilerTaskQueryDAO;
import org.apache.skywalking.oap.server.library.jfr.type.FrameTree;
import org.apache.skywalking.oap.server.library.jfr.type.JFREventType;
import org.apache.skywalking.oap.server.library.jfr.parser.JFRParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

import static org.apache.skywalking.oap.server.receiver.asyncprofiler.provider.handler.AsyncProfilerServiceHandler.parseMetaData;
import static org.apache.skywalking.oap.server.receiver.asyncprofiler.provider.handler.AsyncProfilerServiceHandler.recordAsyncProfilerTaskLog;

@Slf4j
public class AsyncProfilerByteBufCollectionObserver implements StreamObserver<AsyncProfilerData> {
    private final IAsyncProfilerTaskQueryDAO taskDAO;
    private final SourceReceiver sourceReceiver;
    private final int jfrMaxSize;
    private final StreamObserver<AsyncProfilerCollectionResponse> responseObserver;

    private AsyncProfilerCollectionMetaData taskMetaData;
    private ByteBuffer buf;

    public AsyncProfilerByteBufCollectionObserver(IAsyncProfilerTaskQueryDAO taskDAO,
                                                  StreamObserver<AsyncProfilerCollectionResponse> responseObserver,
                                                  SourceReceiver sourceReceiver, int jfrMaxSize) {
        this.sourceReceiver = sourceReceiver;
        this.taskDAO = taskDAO;
        this.responseObserver = responseObserver;
        this.jfrMaxSize = jfrMaxSize;
    }

    @Override
    @SneakyThrows
    public void onNext(AsyncProfilerData asyncProfilerData) {
        if (Objects.isNull(taskMetaData) && asyncProfilerData.hasMetaData()) {
            taskMetaData = parseMetaData(asyncProfilerData.getMetaData(), taskDAO);
            if (AsyncProfilingStatus.PROFILING_SUCCESS.equals(taskMetaData.getType())) {
                int size = taskMetaData.getContentSize();
                if (jfrMaxSize >= size) {
                    buf = ByteBuffer.allocate(size);
                    // Not setting type means telling the client that it can upload jfr files
                    responseObserver.onNext(AsyncProfilerCollectionResponse.newBuilder().build());
                } else {
                    responseObserver.onNext(AsyncProfilerCollectionResponse.newBuilder().setType(AsyncProfilingStatus.TERMINATED_BY_OVERSIZE).build());
                    // The size of the uploaded jfr file exceeds the acceptable range of the oap server
                    recordAsyncProfilerTaskLog(taskMetaData.getTask(), taskMetaData.getInstanceId(),
                            AsyncProfilerTaskLogOperationType.JFR_UPLOAD_FILE_TOO_LARGE_ERROR);
                }
            } else {
                recordAsyncProfilerTaskLog(taskMetaData.getTask(), taskMetaData.getInstanceId(),
                        AsyncProfilerTaskLogOperationType.EXECUTION_TASK_ERROR);
            }
        } else if (asyncProfilerData.hasContent()) {
            asyncProfilerData.getContent().copyTo(buf);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        Status status = Status.fromThrowable(throwable);
        if (Status.CANCELLED.getCode() == status.getCode()) {
            if (log.isDebugEnabled()) {
                log.debug(throwable.getMessage(), throwable);
            }
            return;
        }
        log.error("Error in receiving async profiler profiling data", throwable);
    }

    @Override
    @SneakyThrows
    public void onCompleted() {
        responseObserver.onCompleted();
        if (Objects.nonNull(buf)) {
            buf.flip();
            parseAndStorageData(taskMetaData, buf);
        }
    }

    private void parseAndStorageData(AsyncProfilerCollectionMetaData taskMetaData, ByteBuffer buf) throws IOException {
        AsyncProfilerTask task = taskMetaData.getTask();
        if (task == null) {
            log.error("AsyncProfiler instanceId:{} has not been assigned a task but still uploaded data", taskMetaData.getInstanceId());
            return;
        }

        recordAsyncProfilerTaskLog(task, taskMetaData.getInstanceId(), AsyncProfilerTaskLogOperationType.EXECUTION_FINISHED);

        parseJFRAndStorage(taskMetaData, buf);
    }

    public void parseJFRAndStorage(AsyncProfilerCollectionMetaData taskMetaData,
                                   ByteBuffer buf) throws IOException {
        AsyncProfilerTask task = taskMetaData.getTask();
        Arguments arguments = new Arguments();
        Map<JFREventType, FrameTree> event2treeMap = JFRParser.dumpTree(buf, arguments);
        for (Map.Entry<JFREventType, FrameTree> entry : event2treeMap.entrySet()) {
            JFREventType event = entry.getKey();
            FrameTree tree = entry.getValue();
            JFRProfilingData data = new JFRProfilingData();
            data.setEventType(event);
            data.setFrameTree(tree);
            data.setTaskId(task.getId());
            data.setInstanceId(taskMetaData.getInstanceId());
            data.setUploadTime(taskMetaData.getUploadTime());
            sourceReceiver.receive(data);
        }
    }
}
