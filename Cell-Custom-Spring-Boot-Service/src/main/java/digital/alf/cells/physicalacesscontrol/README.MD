
### Promt

You need to write a Java algorithm to parse a Kyverno policy and additional employee information, then output an Access Control List (ACL) that specifies the permissions for each employee.

Input Information:
Kyverno Policy: Located at resources\physical-access-control\pip-abac-policy1.yml. This policy defines:

Which actions the employee-group can perform.

The conditions under which actions are granted.

Employee Training/Group Information: Located at resources\physical-access-control\pip-info-employee-vde-trainings.yml. This defines:

Which employee belongs to the employee-group.

Which employee has training-vde available.

Task:
Parse the Kyverno policy and the employee information.

Combine the information to determine the final, effective permissions for each individual employee.

Output the resulting permissions in the following Target ACL Format:

- Principal <UsedId:name>
- Action
- Resource
- Condition
  Your Role:
  Act as a senior Java programmer suggesting the parsing algorithm.

Would you like me to suggest an approach for the Java algorithm?

Calling `http://localhost:8080/api/acl/generate`

#### Result 

````json
[
  {
    "principal": "\u003CES-4902:Anya Sharma\u003E",
    "action": "ENTER",
    "resource": "Facility [location='production-room']",
    "condition": "Subject must be member of 'employee-group' AND 'training-vde-available-group'; Time window: 2025-10-20T08:00:00Z to 2025-10-20T09:00:00Z"
  },
  {
    "principal": "\u003CES-4902:Anya Sharma\u003E",
    "action": "EXIT",
    "resource": "Facility [location='production-room']",
    "condition": "Subject must be member of 'employee-group' AND 'training-vde-available-group'; Time window: 2025-10-20T08:00:00Z to 2025-10-20T09:00:00Z"
  },
  {
    "principal": "\u003CDL-1020:David Lee\u003E",
    "action": "ENTER",
    "resource": "Facility [location='production-room']",
    "condition": "Subject must be member of 'employee-group' AND 'training-vde-available-group'; Time window: 2025-10-20T08:00:00Z to 2025-10-20T09:00:00Z"
  },
  {
    "principal": "\u003CDL-1020:David Lee\u003E",
    "action": "EXIT",
    "resource": "Facility [location='production-room']",
    "condition": "Subject must be member of 'employee-group' AND 'training-vde-available-group'; Time window: 2025-10-20T08:00:00Z to 2025-10-20T09:00:00Z"
  }
]````