# agga

Redirect TCP connections through a cluster

## Build

```bash
$ sbt assembly
```

Then, the assembly can be found in the `target/scala-2.12` directory.

## Usage

Launch a new cluster:

```bash
$ java -jar agga-assembly-0.1-SNAPSHOT.jar \
  -Dakka.remote.netty.tcp.hostname=server0.example.com \
  -Dakka.remote.netty.tcp.port=7000 \
  -Dakka.remote.netty.tcp.bind-hostname=192.168.1.100 \
  -Dakka.remote.netty.tcp.bind-port=2552
  -Dakka.cluster.seed-nodes.0=akka.tcp://agga@192.168.1.100:2552
  -Dagga.tcp-redir.hostname=127.0.0.1 \
  -Dagga.tcp-redir.port=1080 \
  -Dagga.accepted-keys.0=client-key-0 \
  -Dagga.accepted-keys.1=client-key-1
```

Join a cluster:

```bash
$ java -jar agga-assembly-0.1-SNAPSHOT.jar \
  -Dakka.remote.netty.tcp.hostname=server1.example.com \
  -Dakka.remote.netty.tcp.port=7000 \
  -Dakka.remote.netty.tcp.bind-hostname=192.168.1.101 \
  -Dakka.remote.netty.tcp.bind-port=2552
  -Dakka.cluster.seed-nodes.0=akka.tcp://agga@192.168.1.100:2552
  -Dagga.tcp-redir.hostname=127.0.0.1 \
  -Dagga.tcp-redir.port=1080 \
  -Dagga.accepted-keys.0=client-key-2 \
  -Dagga.accepted-keys.1=client-key-3
```

## Client

See [sticnarf/agga-client-scala](https://github.com/sticnarf/agga-client-scala).