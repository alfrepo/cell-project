package digital.alf.cells.physicalacesscontrolopa.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpaUserInfoTest {

    @Test
    void getUserId_nullRequest_returnsNull() {
        OpaUserInfo info = new OpaUserInfo(null);
        assertNull(info.getUserId());
    }

    @Test
    void getUserId_nullUserInfo_returnsNull() {
        OpaUserInfo.Request request = new OpaUserInfo.Request(null, null, null, null);
        OpaUserInfo info = new OpaUserInfo(request);
        assertNull(info.getUserId());
    }

    @Test
    void getUserId_validUserInfo_returnsUid() {
        OpaUserInfo.UserInfo userInfo = new OpaUserInfo.UserInfo("Anya Sharma", "ES-4902", List.of());
        OpaUserInfo.Request request = new OpaUserInfo.Request(null, null, null, userInfo);
        OpaUserInfo info = new OpaUserInfo(request);
        assertEquals("ES-4902", info.getUserId());
    }

    @Test
    void getUsername_nullRequest_returnsNull() {
        OpaUserInfo info = new OpaUserInfo(null);
        assertNull(info.getUsername());
    }

    @Test
    void getUsername_validUserInfo_returnsUsername() {
        OpaUserInfo.UserInfo userInfo = new OpaUserInfo.UserInfo("Anya Sharma", "ES-4902", List.of());
        OpaUserInfo.Request request = new OpaUserInfo.Request(null, null, null, userInfo);
        OpaUserInfo info = new OpaUserInfo(request);
        assertEquals("Anya Sharma", info.getUsername());
    }

    @Test
    void getUserGroups_nullRequest_returnsEmptyList() {
        OpaUserInfo info = new OpaUserInfo(null);
        assertEquals(List.of(), info.getUserGroups());
    }

    @Test
    void getUserGroups_nullGroups_returnsEmptyList() {
        OpaUserInfo.UserInfo userInfo = new OpaUserInfo.UserInfo("Anya Sharma", "ES-4902", null);
        OpaUserInfo.Request request = new OpaUserInfo.Request(null, null, null, userInfo);
        OpaUserInfo info = new OpaUserInfo(request);
        assertEquals(List.of(), info.getUserGroups());
    }

    @Test
    void getUserGroups_validGroups_returnsList() {
        List<String> groups = List.of("employee-group", "training-vde-available-group");
        OpaUserInfo.UserInfo userInfo = new OpaUserInfo.UserInfo("Anya Sharma", "ES-4902", groups);
        OpaUserInfo.Request request = new OpaUserInfo.Request(null, null, null, userInfo);
        OpaUserInfo info = new OpaUserInfo(request);
        assertEquals(groups, info.getUserGroups());
    }
}
