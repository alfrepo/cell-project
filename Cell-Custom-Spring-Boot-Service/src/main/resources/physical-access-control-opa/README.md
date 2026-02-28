

## Install

``` shell
sudo snap install open-policy-agent

```


``` bash


#Evaluate

  # Check a user (expected: true)
  opa eval -d policy.rego --input pip-users/pip-userinfo-anya-sharma.json 'data.physical_access_control.allow'

  # Check a user (expected: false + violation message)
  opa eval -d policy.rego --input pip-users/pip-userinfo-ben-carter.json 'data.physical_access_control.deny'
      


```



#### example of a policy report

```json

{
  "result": [
    {
      "expressions": [
        {
          "value": true,
          "text": "data.physical_access_control.deny",
          "location": {
            "row": 1,
            "col": 1
          }
        }
      ]
    }
  ]
}


```