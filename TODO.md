## Todo List

- [ ] Test health check functionality
- [ ] Design a flexible way to add servers at startup instead of hard-coding them
- [ ] Implement endpoints to update the server list dynamically at runtime (consider using another controller)
- [ ] Enhance health checks by making requests parallel 
- [ ] Perform stress testing using wrk
- [ ] Experiment with converting `HealthCheckResult` to a record
- [ ] Write a unit test for every method in the load balancer
- [ ] Abstract the load balancing algorithm into a strategy pattern
- [ ] Refactor business logic out of the controller and into dedicated services
- [ ] Implement a retry policy for handling intermittent server failures gracefully
- [ ] Add analytics functionality to monitor server performance metrics such as latency
