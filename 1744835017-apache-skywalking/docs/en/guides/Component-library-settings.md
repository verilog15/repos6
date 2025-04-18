# Component library settings
Component library settings are about your own or third-party libraries used in the monitored application.

In agent or SDK, regardless of whether the library name is collected as ID or String (literally, e.g. SpringMVC), the collector
formats data in ID for better performance and less storage requirements. 

Also, the collector conjectures the remote service based on the component library. For example: if
the component library is MySQL Driver library, then the remote service should be MySQL Server. 

For these two reasons, the collector requires two parts of settings in this file:
1. Component library ID, names and languages.
1. Remote server mapping based on the local library.

**All component names and IDs must be defined in this file.**

## Component Library ID
Define all names and IDs from component libraries which are used in the monitored application.
This uses a two-way mapping strategy. The agent or SDK could use the value (ID) to represent the component name in uplink data.

- Name: the component name used in agent and UI
- ID: Unique ID. All IDs are reserved once they are released.
- Languages: Program languages may use this component. Multi languages should be separated by `,`.

### ID rules
- Java and multi languages shared: (0, 3000)
- .NET Platform reserved: [3000, 4000)
- Node.js Platform reserved: [4000, 5000)
- Go reserved: [5000, 6000)
- Lua reserved: [6000, 7000)
- Python reserved: [7000, 8000)
- PHP reserved: [8000, 9000)
- C++ reserved: [9000, 10000)
- Javascript reserved: [10000, 11000)
- Rust reserved: [11000, 12000)

Example:
```yaml
Tomcat:
  id: 1
  languages: Java
HttpClient:
  id: 2
  languages: Java,C#,Node.js
Dubbo:
  id: 3
  languages: Java
H2:
  id: 4
  languages: Java
```

## Component Library Priority
Component ID priority represents the component is degree of closeness between the library and business codes
The higher the atomic number, the higher the priority, which mean it is closer to the business codes,
further away from OS kernel or general Computer Science concept.

The range of priorities is [0, 100], both sided included. 0 is the lowest priority.
To keep forward compatibility, the default(when not set) priority is 50.

For example, a typical priority sequence is TCP < TLS(TCP) < RPC < HTTP < HTTPS < gRPC/SpringMVC/Dubbo

Example:
```yaml
Unknown:
  id: 0
  language: All
  priority: 0
tcp:
  id: 110
  languages: Java
  priority: 10
https:
  id: 129
  languages: ebpf
  priority: 46
tls:
  id: 130
  languages: ebpf, mesh
  priority: 11 
```

## Remote server mapping
The remote server will be conjectured by the local component. The mappings are based on names in the component library.

- Key: client component library name
- Value: server component name

```yaml
Component-Server-Mappings:
  Jedis: Redis
  StackExchange.Redis: Redis
  Redisson: Redis
  Lettuce: Redis
  Zookeeper: Zookeeper
  SqlClient: SqlServer
  Npgsql: PostgreSQL
  MySqlConnector: Mysql
  EntityFrameworkCore.InMemory: InMemoryDatabase
```
