package service;

import application.StatusListener;
import data.AlarmStatus;
import data.ArmingStatus;
import data.SecurityRepository;
import data.Sensor;
import data.SensorType;
import imageService.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Unit test for simple App.
 */
@ExtendWith({MockitoExtension.class})
public class SecurityServiceTest
{
    private SecurityService securityService;
    private Sensor sensor;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;

    private Set<Sensor> createSensors(int count, Boolean status){
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < count; i++) {
            sensors.add(new Sensor( randomInt(), SensorType.DOOR));
        }

        sensors.forEach(sensor -> sensor.setActive(status));
        return sensors;
    }

    private String randomInt(){
        Random rnd = new Random();
       return String.valueOf(rnd.nextInt());
    }

    @BeforeEach
    void init(){
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor(randomInt(), SensorType.DOOR);
    }

    /**
     * Rigorous Test :-)
     */

    @Test
    @DisplayName("Alarm is armed and a sensor becomes activated, put the system into pending")
    public void alarmedArmed_sensorActive_pendingalarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);

    }

    @Test
    @DisplayName("armed and a sensor becomes activated and the system is already pending alarm, set off the alarm")
    public void alarmedArmed_sensorActive_pendingAlarm_alarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("pending alarm and all sensors are inactive, return to no alarm state")
    public void pendingalarm_nosensors_noAlarm(){
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    @ParameterizedTest
    @DisplayName("alarm is active, change in sensor state should not affect the alarm state")
    @ValueSource(booleans = {true, false})
    public void alarmedArmed_changesensor_noAffect(boolean status){
        sensor.setActive(!status);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, status);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    @DisplayName("sensor is activated, while already active and the system is in pending state, change it to alarm state")
    public void alarmedArmed_sensorActive_pendingalarm_alarmstate(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("sensor is deactivated while already inactive, make no changes to the alarm state")
    public void sensordeActive_nochange(){
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    @DisplayName("camera image contains a cat while the system is armed-home, put the system into alarm status")
    public void imageCat_armed_alarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("camera image does not contain a cat, change the status to no alarm as long as the sensors are not active")
    public void imagenoCat_senornotactive_noalarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("camera image does not contain a cat, status alarm  as the sensors are active")
    public void imagenoCat_senoractive_alarm(){
        sensor.setActive(true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        when(securityService.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    @DisplayName("system is disarmed, set the status to no alarm")
    public void disarmed_noalarm(){
       securityService.setArmingStatus(ArmingStatus.DISARMED);
       verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @DisplayName("system is armed, reset all sensors to inactive")
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    public void armed_resetSesnorinactive(ArmingStatus armingStatus){
        Set<Sensor> sensors = createSensors(2,true);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(armingStatus);
        securityService.getSensors().forEach(s -> assertFalse(s.getActive()));
    }

    @Test
    @DisplayName("system is armed-home while the camera shows a cat, set the alarm status to alarm")
    public void imagehasCat_armed_alarm(){
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityService.getSensors()).thenReturn(sensors);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("removing senors")
    public void addRemoveSensor(){
        Sensor sensor1 = new Sensor(randomInt(), SensorType.DOOR);
        securityService.addSensor(sensor1);
        securityService.removeSensor(sensor1);
        verify(securityRepository, times(1)).addSensor(sensor1);
        verify(securityRepository, times(1)).removeSensor(sensor1);
    }

    @Test
    @DisplayName("add/remove listeners")
    public void addRemoveListeners() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

}
