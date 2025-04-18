/*
 * Copyright (C) 2025 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.licensee

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
internal abstract class AssetCopyTask : DefaultTask() {
  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val inputFile: RegularFileProperty

  @get:Input
  abstract val targetFileName: Property<String>

  @TaskAction
  fun action() {
    inputFile.get().asFile.copyTo(
      target = File(
        outputDirectory.dir("app/cash/licensee").get().asFile,
        targetFileName.get(),
      ),
      overwrite = true,
    )
  }
}
