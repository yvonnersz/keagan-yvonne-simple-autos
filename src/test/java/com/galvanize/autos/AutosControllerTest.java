package com.galvanize.autos;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AutosController.class)
public class AutosControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AutoService autoService;

    List<Auto> autos;

    @BeforeEach
    void setUp() {
        autos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Auto auto = new Auto(1990, "ford", "t", "red", "123456789" + i);
            autos.add(auto);
        }
    }

    public String toJSON(Object auto) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(auto);
    }

    // GET /api/autos 200: at least one auto exists returns list of all autos matching queries
    @Test
    void getAllAutos_noParams_returnsList() throws Exception {
        when(autoService.getAllAutos()).thenReturn(new AutosList(autos));
        System.out.print(autos);

        mockMvc.perform(get("/api/autos"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autos", hasSize(10)));
    }

    // GET /api/autos ->  204: returns no autos found
    @Test
    void getAllAutos_noParams_returnNoContent() throws Exception {
        when(autoService.getAllAutos()).thenReturn(new AutosList());

        mockMvc.perform(get("/api/autos"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAutos_withParams_returnsAutosList() throws Exception {
        when(autoService.getAllAutos(anyString(), anyString())).thenReturn(new AutosList(autos));
        mockMvc.perform(get("/api/autos?make=ford&color=blue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autos", hasSize(10)));
    }

/*
        POST /api/autos
        NO PARAMS
        BODY: takes an automobile schema object
           200 adds auto successfully returns auto
           400 Bad request return error message


     */

    @Test
    void postNewAuto_WithParams_returnNewAuto() throws Exception {
        Auto auto = autos.get(0);
        when(autoService.addAuto(any(Auto.class))).thenReturn(auto);

        mockMvc.perform(post("/api/autos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJSON(auto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("vin").value(auto.getVin()));
    }

    @Test
    void postNewAuto_WithParams_returnsBadRequest() throws Exception {
        Auto auto = autos.get(0);
        when(autoService.addAuto(any(Auto.class))).thenThrow(InvalidAutoException.class);

        mockMvc.perform(post("/api/autos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJSON(auto)))
                .andExpect(status().isBadRequest());
    }

    /*
        GET /api/autos/{vin}
        PATH: {vin} required
          200 returns an auto that matches vin property
          204 auto not found
     */

    @Test
    void getAuto_VIN_returnFoundAuto() throws Exception {
        Auto auto = autos.get(0);
        when(autoService.getAutoByVin(anyString())).thenReturn(auto);

        mockMvc.perform(get("/api/autos/" + auto.getVin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("vin").value(auto.getVin()));
    }

    @Test
    void getAuto_VIN_returnNotFoundAuto() throws Exception {
        Auto auto = autos.get(0);
        when(autoService.getAutoByVin(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/autos/" + auto.getVin()))
                .andExpect(status().isNoContent());
    }

    /*
        PATCH /api/autos/{vin}
        PATH: {vin} required
        BODY: auto properties to be updated
          200 returns an updated auto that matches vin property
          204 auto not found
          400 bad request returns message
          create update onwer object
          probably change color from final
     */

    @Test
    void updateAuto_VinColorOwner_returnsUpdatedAuto() throws Exception {
        Auto auto = autos.get(0);
        auto.setColor("blue");
        auto.setOwner("joe");
        when(autoService.updateAuto(anyString(), anyString(), anyString())).thenReturn(auto);

        mockMvc.perform(patch("/api/autos/" + auto.getVin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJSON(new UpdateOwnerRequest("joe", "blue"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("owner").value("joe"))
                .andExpect(jsonPath("color").value("blue"));
    }

    @Test
    void updateAuto_VinColorOwner_returnsNotFound() throws Exception {
        when(autoService.updateAuto(anyString(), anyString(), anyString())).thenReturn(null);

        mockMvc.perform(patch("/api/autos/vinNotFound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJSON(new UpdateOwnerRequest("joe", "blue"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateAuto_VinColorOwner_returnsBadRequest() throws Exception {
        when(autoService.updateAuto(anyString(), anyString(), anyString())).thenThrow(InvalidAutoException.class);

        mockMvc.perform(patch("/api/autos/vinNotFound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJSON(new UpdateOwnerRequest("joe", "blue"))))
                .andExpect(status().isBadRequest());
    }

    /*
        DELETE /api/autos/{vin}
        PATH: {vin} required
          202 auto delete accepted
          204 auto not found
     */

    @Test
    void deleteAuto_withVin_returns202() throws Exception {
        Auto auto = autos.get(0);

        mockMvc.perform(delete("/api/autos/" + auto.getVin()))
                .andExpect(status().isAccepted());

        verify(autoService).deleteAuto(anyString());

    }

    @Test
    void deleteAuto_withVin_returnsVehicleNotFound() throws Exception {

        doThrow(new AutoNotFoundException()).when(autoService).deleteAuto(anyString());
        mockMvc.perform(delete("/api/autos/vehicleNotFound"))
                .andExpect(status().isNoContent());

    }
}
