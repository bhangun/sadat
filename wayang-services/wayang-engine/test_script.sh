
#!/bin/bash
mvn test -Dtest=WorkflowGraphQLResourceTest 2>&1 | tail -20