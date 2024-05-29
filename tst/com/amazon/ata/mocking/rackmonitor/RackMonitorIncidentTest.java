package com.amazon.ata.mocking.rackmonitor;

import com.amazon.ata.mocking.rackmonitor.clients.warranty.Warranty;
import com.amazon.ata.mocking.rackmonitor.clients.warranty.WarrantyClient;
import com.amazon.ata.mocking.rackmonitor.clients.warranty.WarrantyNotFoundException;
import com.amazon.ata.mocking.rackmonitor.clients.wingnut.WingnutClient;
import com.amazon.ata.mocking.rackmonitor.exceptions.RackMonitorException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;  // initMocks is deprecated, hence the strikeout
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RackMonitorIncidentTest {
    RackMonitor rackMonitor;

    // external classes
/*
    WingnutClient wingnutClient;
    WarrantyClient warrantyClient;
    Rack rack1;
*/
@Mock
    WingnutClient wingnutClient;
@Mock
    WarrantyClient warrantyClient;
@Mock
    Rack rack1;


/*    Server unhealthyServer = new Server("TEST0001");
    Server shakyServer = new Server("TEST0067");*/

    Server aServer = new Server("TEST001");
    Map<Server, Integer> rack1ServerUnits;
    Map<Server,Double> serverHealth;

    @BeforeEach
    void setUp() {
        initMocks(this);
        /*MockitoAnnotations.openMocks(this);*/



     /*   warrantyClient = new WarrantyClient();
        wingnutClient = new WingnutClient();*/

        serverHealth = new HashMap<>();
        rack1ServerUnits = new HashMap<>();
        rack1ServerUnits.put(aServer, 1);
     /*   rack1 = new Rack("RACK01", rack1ServerUnits);*/
        rackMonitor = new RackMonitor(new HashSet<>(Arrays.asList(rack1)),
            wingnutClient, warrantyClient, 0.9D, 0.8D);
    }

    @Test
    public void getIncidents_withOneUnhealthyServer_createsOneReplaceIncident() throws Exception {
        // GIVEN

        serverHealth.put(aServer, .5);
        when(rack1.getHealth()).thenReturn(serverHealth);
        when(rack1.getUnitForServer(aServer)).thenReturn(1);
        when(warrantyClient.getWarrantyForServer(aServer)).thenReturn(Warranty.nullWarranty());
        //when you see this method return this value
        // The rack is set up with a single unhealthy server
        // We've reported the unhealthy server to Wingnut
        rackMonitor.monitorRacks();

        // WHEN
        Set<HealthIncident> actualIncidents = rackMonitor.getIncidents();

        // THEN
        HealthIncident expected =
            new HealthIncident(aServer, rack1, 1, RequestAction.REPLACE);
        assertTrue(actualIncidents.contains(expected),
            "Monitoring an unhealthy server should record a REPLACE incident!");
    }

    @Test
    public void getIncidents_withOneShakyServer_createsOneInspectIncident() throws Exception {
        // GIVEN
        // The rack is set up with a single shaky server
       /* rack1ServerUnits = new HashMap<>();
        rack1ServerUnits.put(aServer, 1);
        rack1 = new Rack("RACK01", rack1ServerUnits);
        rackMonitor = new RackMonitor(new HashSet<>(Arrays.asList(rack1)),
            wingnutClient, warrantyClient, 0.9D, 0.8D);*/
        // We've reported the shaky server to Wingnut
        serverHealth.put(aServer, .85);
        when(rack1.getHealth()).thenReturn(serverHealth);
        when(rack1.getUnitForServer(aServer)).thenReturn(1);
        when(warrantyClient.getWarrantyForServer(aServer)).thenReturn(Warranty.nullWarranty());

        rackMonitor.monitorRacks();

        // WHEN
        Set<HealthIncident> actualIncidents = rackMonitor.getIncidents();

        // THEN
        HealthIncident expected =
            new HealthIncident(aServer, rack1, 1, RequestAction.INSPECT);
        assertTrue(actualIncidents.contains(expected),
            "Monitoring a shaky server should record an INSPECT incident!");
    }

    @Test
    public void getIncidents_withOneHealthyServer_createsNoIncidents() throws Exception {
        // GIVEN
        serverHealth.put(aServer, .91);
        when(rack1.getHealth()).thenReturn(serverHealth);
        when(rack1.getUnitForServer(aServer)).thenReturn(1);
        when(warrantyClient.getWarrantyForServer(aServer)).thenReturn(Warranty.nullWarranty());

        rackMonitor.monitorRacks();
        // monitorRacks() will find only healthy servers

        // WHEN
        Set<HealthIncident> actualIncidents = rackMonitor.getIncidents();

        // THEN
        assertEquals(0, actualIncidents.size(),
            "Monitoring a healthy server should record no incidents!");
    }

    @Test
    public void monitorRacks_withOneUnhealthyServer_replacesServer() throws Exception {
        // GIVEN
        // The rack is set up with a single unhealthy server
        serverHealth.put(aServer, .8);
        when(rack1.getHealth()).thenReturn(serverHealth);
        when(rack1.getUnitForServer(aServer)).thenReturn(1);
        when(warrantyClient.getWarrantyForServer(aServer)).thenReturn(Warranty.nullWarranty());
        // WHEN
        rackMonitor.monitorRacks();

        verify(warrantyClient).getWarrantyForServer(aServer); //verify the methodwas called at least once
        verify(wingnutClient).requestReplacement(rack1,1,Warranty.nullWarranty());

        // THEN
        // There were no exceptions
        // No way to tell we called the warrantyClient for the server's Warranty
        // No way to tell we called Wingnut to replace the server
    }

    @Test
    public void monitorRacks_withUnwarrantiedServer_throwsServerException() throws Exception {
        // GIVEN
       /* Server noWarrantyServer = new Server("TEST0052");
        rack1ServerUnits = new HashMap<>();
        rack1ServerUnits.put(noWarrantyServer, 1);
        rack1 = new Rack("RACK01", rack1ServerUnits);
        rackMonitor = new RackMonitor(new HashSet<>(Arrays.asList(rack1)),
            wingnutClient, warrantyClient, 0.9D, 0.8D);*/

        // WHEN and THEN
        serverHealth.put(aServer, .63);
        when(rack1.getHealth()).thenReturn(serverHealth);
        when(rack1.getUnitForServer(aServer)).thenReturn(1);
        when(warrantyClient.getWarrantyForServer(aServer)).thenThrow(WarrantyNotFoundException.class);


        assertThrows(RackMonitorException.class,
            () -> rackMonitor.monitorRacks(),
            "Monitoring a server with no warranty should throw exception!");
    }
}
