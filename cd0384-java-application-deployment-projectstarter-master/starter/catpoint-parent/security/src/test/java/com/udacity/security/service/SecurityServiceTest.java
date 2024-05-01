package com.udacity.security.service;

import com.udacity.security.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;


@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    FakeImageService imageService;

    @Mock
    SecurityRepository securityRepository;

    @InjectMocks
    SecurityService securityService;

    private Sensor sensor;

    @BeforeEach
    void setUp() {
        securityService =  new SecurityService(securityRepository, imageService);
        sensor = new Sensor("test", SensorType.DOOR);
    }

    public Set<Sensor> getSensorInactive() {
        Set<Sensor> sensors = new HashSet<>();
        Sensor sensor1 = new Sensor("test1", SensorType.DOOR);
        Sensor sensor2 = new Sensor("test2", SensorType.MOTION);
        sensors.add(sensor1);
        sensors.add(sensor2);
        return sensors;
    }

    public Set<Sensor> getSensorActive() {
        Set<Sensor> sensors =getSensorInactive();
        sensors.forEach(p -> p.setActive(true));
        return sensors;
    }

    @ParameterizedTest // 1
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void testArmingStatusChangeWithSensorActivation_PendingAlarmStatus(ArmingStatus armingStatus) {
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        setUpArmingStatusMocks();
        securityService.changeSensorActivationStatus(sensor, true);
        verifyAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @ParameterizedTest //2
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void testSensorActivationDuringPendingAlarm_SetToAlarmStatus(ArmingStatus armingStatus) {
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        setUpPendingAlarmMocks();
        securityService.changeSensorActivationStatus(sensor, true);
        verifyAlarmStatus(AlarmStatus.ALARM);
    }

    @Test //3
    void testInactiveSensorsDuringPendingAlarm_ReturnToNoAlarmState() {
        setUpPendingAlarmMocks();
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verifyAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test //4
    void testSensorStateChangeDuringActiveAlarm_NoEffectOnAlarmState() {
        setUpActiveAlarmMocks();
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        Mockito.verify(securityRepository, Mockito.never()).setAlarmStatus(Mockito.any());
    }

    @Test //5
    void testSensorActivationWhileAlreadyActiveAndSystemPending_ChangesToAlarmState() {
        setUpPendingAlarmMocks();
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verifyAlarmStatus(AlarmStatus.ALARM);
    }

    @Test //6
    void testInactiveSensorDeactivation_NoEffectOnAlarmState() {
        setUpArmingStatusMocks();
        securityService.changeSensorActivationStatus(sensor, false);
        Mockito.verify(securityRepository, Mockito.never()).setAlarmStatus(ArgumentMatchers.any());
    }

    @ParameterizedTest //7
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    void testCatDetectionWhileSystemArmed_PutsSystemIntoAlarmStatus(ArmingStatus armingStatus) {
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        Mockito.when(imageService.imageContainsCat(Mockito.any(), Mockito.anyFloat())).thenReturn(true);
        securityService.processImage(Mockito.mock(BufferedImage.class));
        verifyAlarmStatus(AlarmStatus.ALARM);
    }

    @Test //8
    void testNoCatDetectionWhileSystemArmedHome_ChangesStatusToNoAlarm() {
        Mockito.when(imageService.imageContainsCat(Mockito.any(), Mockito.anyFloat())).thenReturn(false);
        securityService.processImage(Mockito.mock(BufferedImage.class));
        verifyAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test //9
    void testDisarmSystem_ChangesStatusToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verifyAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest //10
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    void testArmSystem_ResetsAllSensorsToInactive(ArmingStatus armingStatus) {
        Set<Sensor> sensors = getSensorActive();
        Mockito.when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(armingStatus);
        securityService.getSensors().forEach(sensor -> Assertions.assertFalse(sensor.getActive()));
    }

    @Test // 11
    void testArmedHomeSystemWithCatDetection_SetsAlarmStatusToAlarm() {
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(imageService.imageContainsCat(Mockito.any(BufferedImage.class), Mockito.anyFloat())).thenReturn(true);
        securityService.processImage(Mockito.mock(BufferedImage.class));
        verifyAlarmStatus(AlarmStatus.ALARM);
    }
    
    private void setUpArmingStatusMocks() {
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
    }

    private void setUpPendingAlarmMocks() {
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    }

    private void setUpActiveAlarmMocks() {
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
    }

    private void verifyAlarmStatus(AlarmStatus alarmStatus) {
        Mockito.verify(securityRepository, Mockito.times(1)).setAlarmStatus(alarmStatus);
    }
}