# Kyverno Policy to ACL Conversion Algorithm

## Overview

This Java implementation parses Kyverno ABAC (Attribute-Based Access Control) policies and generates Access Control Lists (ACLs) based on employee training data.

## Algorithm Components

### 1. Data Models (`model/`)

#### `EmployeeInfo.java`
Represents an employee with:
- `id`: Employee identifier (e.g., "ES-4902")
- `name`: Employee full name
- `groups`: Map of group memberships with boolean values

#### `KyvernoPolicyData.java`
Extracted policy information:
- `operations`: List of allowed operations (ENTER, EXIT)
- `resourceKind`: Type of resource (Facility)
- `resourceLabels`: Resource attributes (location: production-room)
- `matchGroup`: Subject group requirement (employee-group)
- `requiredGroup`: Required attribute (training-vde-available-group)
- `timeWindowStart/End`: Temporal constraints

#### `AclEntry.java`
Output format:
- `principal`: `<UserId:name>`
- `action`: Operation allowed
- `resource`: Resource description
- `condition`: Access conditions

### 2. Parsers (`parser/`)

#### `KyvernoPolicyParser.java`
Parses Kyverno policy YAML structure:
```java
1. Extract metadata (policy name)
2. Extract match criteria:
   - Subject groups
   - Operations
   - Resource types and labels
3. Extract preconditions (time windows)
4. Extract deny conditions to determine required attributes
5. Parse temporal expressions using regex
```

#### `EmployeeInfoParser.java`
Parses employee training data:
```java
1. Load YAML structure
2. Iterate through employees list
3. Extract id, name, and group memberships
4. Create EmployeeInfo objects
```

### 3. Generator (`generator/`)

#### `AclGenerator.java`
Core algorithm logic:

```java
Algorithm: generateAcl(policyData, employees)
-------------------------------------------
Input: KyvernoPolicyData, List<EmployeeInfo>
Output: List<AclEntry>

Steps:
1. Filter employees who meet ALL requirements:
   - Must be member of matchGroup (employee-group)
   - Must be member of requiredGroup (training-vde-available-group)

2. For each qualified employee:
   - For each operation in policy:
     - Create AclEntry with:
       * principal = formatPrincipal(employee)
       * action = operation
       * resource = buildResourceDescription(policy)
       * condition = buildConditionDescription(policy)

3. Return list of ACL entries
```

**Key Logic**: The Kyverno policy uses DENY semantics:
- DENY IF: (user IN employee-group) AND (user NOT IN training-vde-available-group)
- Therefore, ALLOW IF: (user IN employee-group) AND (user IN training-vde-available-group)

### 4. Main Strategy (`PolicyToAclStrategy.java`)

Orchestrates the entire conversion process:

```java
Main Algorithm: convertPolicyToAcl()
-----------------------------------
1. Parse Kyverno policy from resources/physical-access-control/pip-abac-policy1.yml
2. Parse employee data from resources/physical-access-control/pip-info-employee-vde-trainings.yml
3. Generate ACL entries using AclGenerator
4. Return formatted ACL entries
```

## Usage

### Via REST API

```bash
# Get ACL as JSON
GET http://localhost:8080/api/acl/generate

# Get formatted ACL as text
GET http://localhost:8080/api/acl/generate/formatted
```

### Programmatically

```java
@Autowired
private PolicyToAclStrategy policyToAclStrategy;

public void generateAcl() throws IOException {
    List<AclEntry> aclEntries = policyToAclStrategy.convertPolicyToAcl();
    String formatted = policyToAclStrategy.formatAclOutput(aclEntries);
    System.out.println(formatted);
}
```

## Example Output

Given the test data:
- **Policy**: Requires `employee-group` AND `training-vde-available-group` for ENTER/EXIT to production-room Facility
- **Employees**: 5 total, 2 with both required groups (Anya Sharma, David Lee)

**Generated ACL**:
```
================================================================================
ACCESS CONTROL LIST (ACL)
Generated from Kyverno ABAC Policy
================================================================================

Total entries: 4

Entry #1
- Principal: <ES-4902:Anya Sharma>
- Action: ENTER
- Resource: Facility [location='production-room']
- Condition: Subject must be member of 'employee-group' AND 'training-vde-available-group'; Time window: 2025-10-20T08:00:00Z to 2025-10-20T09:00:00Z

Entry #2
- Principal: <ES-4902:Anya Sharma>
- Action: EXIT
- Resource: Facility [location='production-room']
- Condition: Subject must be member of 'employee-group' AND 'training-vde-available-group'; Time window: 2025-10-20T08:00:00Z to 2025-10-20T09:00:00Z

Entry #3
- Principal: <DL-1020:David Lee>
- Action: ENTER
- Resource: Facility [location='production-room']
- Condition: Subject must be member of 'employee-group' AND 'training-vde-available-group'; Time window: 2025-10-20T08:00:00Z to 2025-10-20T09:00:00Z

Entry #4
- Principal: <DL-1020:David Lee>
- Action: EXIT
- Resource: Facility [location='production-room']
- Condition: Subject must be member of 'employee-group' AND 'training-vde-available-group'; Time window: 2025-10-20T08:00:00Z to 2025-10-20T09:00:00Z

================================================================================
```

## Architecture Decisions

### Why SnakeYAML?
- Already included in Spring Boot
- Mature library for YAML parsing
- Supports complex nested structures

### Why separate parsers?
- Single Responsibility Principle
- Easy to test independently
- Flexible for different policy formats

### Why convert DENY to ALLOW logic?
- ACLs typically express positive permissions
- More intuitive for access control lists
- Kyverno uses deny policies, we generate allow lists

### Performance Considerations
- Streaming YAML parsing with try-with-resources
- Single pass through employee list
- O(n * m) complexity where n=employees, m=operations
- For large datasets, consider caching parsed policies

## Testing

Run tests with:
```bash
./gradlew test --tests PolicyToAclStrategyTest
```

Test verifies:
- Correct number of ACL entries generated
- Only qualified employees included
- All operations covered
- Resource and condition formatting
- Principal format compliance

## Extension Points

To support additional policy types:
1. Create new parser implementing similar pattern
2. Extend `KyvernoPolicyData` with new attributes
3. Update `AclGenerator` logic for new conditions
4. Add integration test

To support different employee data sources:
1. Implement new parser for EmployeeInfo
2. Maintain same EmployeeInfo interface
3. Inject appropriate parser via Spring

## File Structure

```
src/main/java/digital/alf/cells/physicalacesscontrol/
├── PolicyToAclStrategy.java           # Main orchestrator
├── controller/
│   └── AclController.java             # REST endpoints
├── generator/
│   └── AclGenerator.java              # ACL generation logic
├── model/
│   ├── AclEntry.java                  # Output model
│   ├── EmployeeInfo.java              # Employee data model
│   └── KyvernoPolicyData.java         # Policy data model
└── parser/
    ├── EmployeeInfoParser.java        # Employee YAML parser
    └── KyvernoPolicyParser.java       # Kyverno policy parser

src/main/resources/physical-access-control/
├── pip-abac-policy1.yml               # Kyverno policy
└── pip-info-employee-vde-trainings.yml # Employee training data

src/test/java/digital/alf/cells/physicalacesscontrol/
└── PolicyToAclStrategyTest.java       # Integration test
```
