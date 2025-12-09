# Introduction
Performance tests for PostgreSQL and Cassandra (single node)


# Installation / Test
- Run docker compose: docker-compose up
- Enter container: docker exec -it dq_cassandra /bin/bash 
  - Execute: cqlsh -u cassandra -p cassandra
- Or outside the container: cqlsh 127.0.0.1 9042 -u cassandra -p cassandra
- Check with SQL: SELECT data_center FROM system.local;


# ToDo
- trace https://stackoverflow.com/questions/34075261/cassandra-query-execution-time-analysis
