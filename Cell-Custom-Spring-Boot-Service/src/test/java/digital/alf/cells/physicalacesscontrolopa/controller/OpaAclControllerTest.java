package digital.alf.cells.physicalacesscontrolopa.controller;

import digital.alf.cells.physicalacesscontrolopa.OpaPolicyToAclStrategy;
import digital.alf.cells.physicalacesscontrolopa.model.AclEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpaAclController.class)
class OpaAclControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpaPolicyToAclStrategy opaPolicyToAclStrategy;

    private List<AclEntry> twoEntries() {
        return List.of(
                AclEntry.builder().principal("<ES-4902:Anya Sharma>").action("ENTER").resource("Facility").condition("has training-vde").build(),
                AclEntry.builder().principal("<DL-1020:David Lee>").action("ENTER").resource("Facility").condition("has training-vde").build()
        );
    }

    // --- GET /api/acl/opa/generate ---

    @Test
    @WithMockUser
    void generateAcl_success_returns200WithJsonBody() throws Exception {
        when(opaPolicyToAclStrategy.convertPolicyToAcl()).thenReturn(twoEntries());

        mockMvc.perform(get("/api/acl/opa/generate").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].principal").value("<ES-4902:Anya Sharma>"))
                .andExpect(jsonPath("$[0].action").value("ENTER"))
                .andExpect(jsonPath("$[0].resource").value("Facility"))
                .andExpect(jsonPath("$[1].principal").value("<DL-1020:David Lee>"));
    }

    @Test
    @WithMockUser
    void generateAcl_emptyResult_returns200WithEmptyArray() throws Exception {
        when(opaPolicyToAclStrategy.convertPolicyToAcl()).thenReturn(List.of());

        mockMvc.perform(get("/api/acl/opa/generate").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    void generateAcl_serviceThrowsIOException_returns500() throws Exception {
        when(opaPolicyToAclStrategy.convertPolicyToAcl()).thenThrow(new IOException("file not found"));

        mockMvc.perform(get("/api/acl/opa/generate").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void generateAcl_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/acl/opa/generate").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/acl/opa/generate/formatted ---

    @Test
    @WithMockUser
    void generateFormattedAcl_success_returns200WithText() throws Exception {
        when(opaPolicyToAclStrategy.convertPolicyToAcl()).thenReturn(twoEntries());
        when(opaPolicyToAclStrategy.formatAclOutput(twoEntries()))
                .thenReturn("ACCESS CONTROL LIST\nEntry #1\n");

        mockMvc.perform(get("/api/acl/opa/generate/formatted"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void generateFormattedAcl_serviceThrowsIOException_returns500() throws Exception {
        when(opaPolicyToAclStrategy.convertPolicyToAcl()).thenThrow(new IOException("fail"));

        mockMvc.perform(get("/api/acl/opa/generate/formatted"))
                .andExpect(status().isInternalServerError());
    }

    // --- GET /api/acl/opa/generate/dynamic ---

    @Test
    @WithMockUser
    void generateAclDynamic_success_returns200WithJsonBody() throws Exception {
        when(opaPolicyToAclStrategy.convertPolicyToAclWithDynamicEvaluation()).thenReturn(twoEntries());

        mockMvc.perform(get("/api/acl/opa/generate/dynamic").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser
    void generateAclDynamic_serviceThrowsIOException_returns500() throws Exception {
        when(opaPolicyToAclStrategy.convertPolicyToAclWithDynamicEvaluation()).thenThrow(new IOException("opa not found"));

        mockMvc.perform(get("/api/acl/opa/generate/dynamic").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    // --- GET /api/acl/opa/generate/dynamic/formatted ---

    @Test
    @WithMockUser
    void generateFormattedAclDynamic_success_returns200() throws Exception {
        when(opaPolicyToAclStrategy.convertPolicyToAclWithDynamicEvaluation()).thenReturn(twoEntries());
        when(opaPolicyToAclStrategy.formatAclOutput(twoEntries())).thenReturn("formatted output");

        mockMvc.perform(get("/api/acl/opa/generate/dynamic/formatted"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void generateFormattedAclDynamic_serviceThrowsIOException_returns500() throws Exception {
        when(opaPolicyToAclStrategy.convertPolicyToAclWithDynamicEvaluation()).thenThrow(new IOException("fail"));

        mockMvc.perform(get("/api/acl/opa/generate/dynamic/formatted"))
                .andExpect(status().isInternalServerError());
    }
}
