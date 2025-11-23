
``` bash

kyverno apply pip-abac-policy1.yml   --resource pip-resources/pip-resource-anya-sharma.yml -t


kyverno apply   --policy pip-abac-policy1.yml  --resource pip-resources/pip-resource-room.yml  --userinfo pip-userinfo-anya-sharma.yml   --operation ENTER   --context-entry '{"key":"time_now_utc()", "value":"2025-10-20T08:15:00Z"} -t


kyverno apply   --policy pip-abac-policy1.yml  --resource pip-resources/pip-resource-room.yml  --userinfo pip-userinfo-anya-sharma.yml   --operation ENTER   --context-entry '{"key":"time_now_utc()", "value":"2025-10-20T08:15:00Z"} -t


kyverno apply
 pip-abac-policy1.yml   --resource pip-resources/pip-resource-room.yml   --userinfo "pip-resources/pip-userinfo-anya-sharma.yml"   --set request.operation=UPDATE     -t
'


Applying 1 policy rule(s) to 1 resource(s)...
│────│─────────────────────│───────────────────────│──────────────────────────────│────────│────────│
│ ID │ POLICY              │ RULE                  │ RESOURCE                     │ RESULT │ REASON │
│────│─────────────────────│───────────────────────│──────────────────────────────│────────│────────│
│ 1  │ require-hello-label │ check-for-hello-label │ default/UserInfo/anya-sharma │ Fail   │        │
│────│─────────────────────│───────────────────────│──────────────────────────────│────────│────────│

```