

## Install

``` shell
curl -LO https://github.com/kyverno/kyverno/releases/download/v1.16.0/kyverno-cli_v1.16.0_linux_x86_64.tar.gz
tar -xvf kyverno-cli_v1.16.0_linux_x86_64.tar.gz
sudo cp kyverno /usr/local/bin/

# windows powershell
$url = 'https://github.com/kyverno/kyverno/releases/download/v1.16.0/kyverno-cli_v1.16.0_windows_x86_64.zip'; $zipFile = "$env:TEMP\kyverno-cli.zip"; $extractDir = "$env:TEMP\kyverno-cli-extracted"; Invoke-WebRequest -Uri $url -OutFile $zipFile; Expand-Archive -Path $zipFile -DestinationPath $extractDir -Force; Move-Item -Path "$extractDir\kyverno.exe" -Destination "C:\Windows\System32\kyverno.exe" -Force; Remove-Item -Path $zipFile -Force; Remove-Item -Path $extractDir -Recurse -Force

```


``` bash

# linux
kyverno apply pip-abac-policy1.yml   \
    --resource pip-resources/pip-resource-room.yml   \
    --userinfo "pip-users/pip-userinfo-anya-sharma.yml"   \
    --set request.operation=UPDATE,request.time.admissionTime="2025-10-20T08:30:00Z"     -t --policy-report

# windows
kyverno apply pip-abac-policy1.yml --resource pip-resources/pip-resource-room.yml --userinfo "pip-users/pip-userinfo-anya-sharma.yml" --set request.operation=UPDATE --set request.time.admissionTime="2025-10-20T08:30:00Z" -t --policy-report


Applying 1 policy rule(s) to 1 resource(s)...
│────│────────────────────────────────────│──────────────────────────────────│─────────────────────────────────────│────────│────────│
│ ID │ POLICY                             │ RULE                             │ RESOURCE                            │ RESULT │ REASON │
│────│────────────────────────────────────│──────────────────────────────────│─────────────────────────────────────│────────│────────│
│ 1  │ abac-enroll-restriction-time-bound │ deny-enroll-without-training-vde │ default/Facility/main-entrance-door │ Pass   │        │
│────│────────────────────────────────────│──────────────────────────────────│─────────────────────────────────────│────────│────────│

#linux
kyverno apply pip-abac-policy1.yml   \
    --resource pip-resources/pip-resource-room.yml   \
    --userinfo "pip-users/pip-userinfo-ben-carter.yml"   \
    --set request.operation=UPDATE,request.time.admissionTime="2025-10-20T08:30:00Z"     -t --policy-report

# windows
kyverno apply pip-abac-policy1.yml --resource pip-resources/pip-resource-room.yml --userinfo "pip-users/pip-userinfo-ben-carter.yml" --set "request.operation=UPDATE,request.time.admissionTime=2025-10-20T08:30:00Z" -t  --policy-report --output-format json
    

Applying 1 policy rule(s) to 1 resource(s)...
│────│────────────────────────────────────│──────────────────────────────────│─────────────────────────────────────│────────│────────│
│ ID │ POLICY                             │ RULE                             │ RESOURCE                            │ RESULT │ REASON │
│────│────────────────────────────────────│──────────────────────────────────│─────────────────────────────────────│────────│────────│
│ 1  │ abac-enroll-restriction-time-bound │ deny-enroll-without-training-vde │ default/Facility/main-entrance-door │ Fail   │        │
│────│────────────────────────────────────│──────────────────────────────────│─────────────────────────────────────│────────│────────│

# as json
{"kind":"ClusterReport","apiVersion":"openreports.io/v1alpha1","metadata":{"name":"merged"},"source":"","summary":{"pass":0,"fail":1,"warn":0,"error":0,"skip":0},"results":[{"source":"kyverno","policy":"abac-enroll-restriction-t
ime-bound","rule":"deny-enroll-without-training-vde","timestamp":{"seconds":1763935458,"nanos":0},"result":"fail","scored":true,"resources":[{"kind":"Facility","namespace":"default","name":"main-entrance-door","apiVersion":"v1"}],"message":"ATTENTION: The 'UPDATE' operation is only allowed for qualified personnel ('training-vde-available') during the critical time window (08:00-09:00 UTC).","properties":{"process":"admission review"}}]}


```

#### example of a policy report

```json
{
    "kind": "ClusterReport",
    "apiVersion": "openreports.io/v1alpha1",
    "metadata": {
        "name": "merged"
    },
    "source": "",
    "summary": {
        "pass": 0,
        "fail": 1,
        "warn": 0,
        "error": 0,
        "skip": 0
    },
    "results": [
        {
            "source": "kyverno",
            "policy": "abac-enroll-restriction-time-bound",
            "rule": "deny-enroll-without-training-vde",
            "timestamp": {
                "seconds": 1763935458,
                "nanos": 0
            },
            "result": "fail",
            "scored": true,
            "resources": [
                {
                    "kind": "Facility",
                    "namespace": "default",
                    "name": "main-entrance-door",
                    "apiVersion": "v1"
                }
            ],
            "message": "ATTENTION: The 'UPDATE' operation is only allowed for qualified personnel ('training-vde-available') during the critical time window (08: 00-09: 00 UTC).",
            "properties": {
                "process": "admission review"
            }
        }
    ]
}
```