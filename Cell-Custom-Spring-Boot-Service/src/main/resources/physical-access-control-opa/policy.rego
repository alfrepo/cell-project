# policy.rego
# Converted from: pip-abac-policy1.yml (kyverno.io/v1 ClusterPolicy)
#
# Policy: abac-enroll-restriction-time-bound
# Restricts the ENTER operation on Facility resources to users possessing
# 'training-vde-available-group'. This restriction is only enforced during
# the time window: 2024-10-20T08:00:00Z to 2026-10-20T19:00:00Z.
#
# Usage:
#   opa eval -d policy.rego --input pip-users/pip-userinfo-anya-sharma.json \
#            'data.physical_access_control.allow'

package physical_access_control

import rego.v1

# ---------------------------------------------------------------------------
# Default: deny unless explicitly allowed
# ---------------------------------------------------------------------------
default allow := false

# Allow if no denial condition triggers
allow if {
    not deny
}

# ---------------------------------------------------------------------------
# Rule: deny-enroll-without-training-vde
#
# Fires when ALL of the following hold:
#   1. The target resource is of kind "Facility"
#   2. The current time falls within the critical time window
#   3. The requesting user does NOT hold 'training-vde-available-group'
# ---------------------------------------------------------------------------
deny if {
    input.request.resource.kind == "Facility"
    time_within_window
    not "training-vde-available-group" in input.request.userInfo.groups
}

# ---------------------------------------------------------------------------
# Temporal precondition helper
# Evaluates to true when the request's admissionTime is within the window.
# ---------------------------------------------------------------------------
time_within_window if {
    admission_time := time.parse_rfc3339_ns(input.request.admissionTime)
    window_start   := time.parse_rfc3339_ns("2024-10-20T08:00:00Z")
    window_end     := time.parse_rfc3339_ns("2026-10-20T19:00:00Z")
    admission_time >= window_start
    admission_time <= window_end
}

# ---------------------------------------------------------------------------
# Violation messages (useful for audit / structured denial responses)
# ---------------------------------------------------------------------------
violation contains msg if {
    deny
    msg := "ATTENTION: The 'ENTER' operation on a Facility is only allowed for qualified personnel ('training-vde-available-group') during the critical time window (2024-10-20 08:00 UTC – 2026-10-20 19:00 UTC)."
}
