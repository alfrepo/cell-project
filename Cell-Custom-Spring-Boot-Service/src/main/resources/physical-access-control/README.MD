
``` bash

kyverno apply pip-abac-policy1.yml   \
    --resource pip-resources/pip-resource-room.yml   \
    --userinfo "pip-resources/pip-userinfo-anya-sharma.yml"   \
    --set request.operation=UPDATE,request.time.admissionTime="2025-10-20T08:30:00Z"     -t


Applying 1 policy rule(s) to 1 resource(s)...
│────│────────────────────────────────────│──────────────────────────────────│─────────────────────────────────────│────────│────────│
│ ID │ POLICY                             │ RULE                             │ RESOURCE                            │ RESULT │ REASON │
│────│────────────────────────────────────│──────────────────────────────────│─────────────────────────────────────│────────│────────│
│ 1  │ abac-enroll-restriction-time-bound │ deny-enroll-without-training-vde │ default/Facility/main-entrance-door │ Pass   │        │
│────│────────────────────────────────────│──────────────────────────────────│─────────────────────────────────────│────────│────────│


kyverno apply pip-abac-policy1.yml   \
    --resource pip-resources/pip-resource-room.yml   \
    --userinfo "pip-resources/pip-userinfo-ben-carter.yml"   \
    --set request.operation=UPDATE,request.time.admissionTime="2025-10-20T08:30:00Z"     -t

Applying 1 policy rule(s) to 1 resource(s)...
│────│────────────────────────────────────│──────────────────────────────────│─────────────────────────────────────│────────│────────│
│ ID │ POLICY                             │ RULE                             │ RESOURCE                            │ RESULT │ REASON │
│────│────────────────────────────────────│──────────────────────────────────│─────────────────────────────────────│────────│────────│
│ 1  │ abac-enroll-restriction-time-bound │ deny-enroll-without-training-vde │ default/Facility/main-entrance-door │ Fail   │        │
│────│────────────────────────────────────│──────────────────────────────────│─────────────────────────────────────│────────│────────│

```