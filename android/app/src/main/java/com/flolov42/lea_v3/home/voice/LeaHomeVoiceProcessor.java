package com.flolov42.lea_v3.home.voice;

import com.flolov42.lea_v3.home.control.DeviceControlManager;
import com.flolov42.lea_v3.home.control.HomeAssistantController;
import com.flolov42.lea_v3.home.database.LeaHomeDatabase;
import com.flolov42.lea_v3.home.models.HomeConfig;
import com.flolov42.lea_v3.home.models.SmartDevice;
import com.flolov42.lea_v3.voice.LeaVoiceCommandManager;

import android.content.Context;

public class LeaHomeVoiceProcessor {

    public interface VoiceResponse {
        void onSpeak(String text);
        void onAction(String description);
    }

    private final CommandParser        parser;
    private final DeviceControlManager control;
    private final HomeAssistantController ha;
    private final LeaVoiceCommandManager voice;

    public LeaHomeVoiceProcessor(Context ctx) {
        LeaHomeDatabase db = LeaHomeDatabase.get(ctx);
        parser  = new CommandParser(db);
        control = DeviceControlManager.get(ctx);
        ha      = HomeAssistantController.get(HomeConfig.get(ctx));
        voice   = LeaVoiceCommandManager.get(ctx);
    }

    public void process(String command, VoiceResponse cb) {
        CommandParser.ParsedCommand cmd = parser.parse(command);

        switch (cmd.action) {
            case TURN_ON:
                if (cmd.resolved != null) executeOn(cmd.resolved, cb);
                else noDevice(command, cb);
                break;

            case TURN_OFF:
                if (cmd.resolved != null) executeOff(cmd.resolved, cb);
                else noDevice(command, cb);
                break;

            case TOGGLE:
                if (cmd.resolved != null) executeToggle(cmd.resolved, cb);
                else noDevice(command, cb);
                break;

            case SET_BRIGHTNESS:
                if (cmd.resolved != null && cmd.brightness >= 0)
                    executeBrightness(cmd.resolved, cmd.brightness, cb);
                break;

            case SET_TEMPERATURE:
                if (cmd.resolved != null && cmd.temperature >= 0)
                    executeTemperature(cmd.resolved, cmd.temperature, cb);
                break;

            case SCENE:
                if (cmd.sceneName != null) executeScene(cmd.sceneName, cb);
                break;

            default:
                voice.speak("Je n'ai pas compris cette commande domotique");
                if (cb != null) cb.onSpeak("Commande non reconnue : " + command);
                break;
        }
    }

    private void executeOn(SmartDevice d, VoiceResponse cb) {
        control.turnOn(d, new DeviceControlManager.ControlCallback() {
            @Override public void onSuccess(SmartDevice dev, String state) {
                String msg = dev.friendlyName + " allumé";
                voice.speak(msg);
                if (cb != null) cb.onAction(msg);
            }
            @Override public void onError(SmartDevice dev, String err) {
                String msg = "Impossible d'allumer " + dev.friendlyName;
                voice.speak(msg);
                if (cb != null) cb.onAction(msg);
            }
        });
    }

    private void executeOff(SmartDevice d, VoiceResponse cb) {
        control.turnOff(d, new DeviceControlManager.ControlCallback() {
            @Override public void onSuccess(SmartDevice dev, String state) {
                String msg = dev.friendlyName + " éteint";
                voice.speak(msg);
                if (cb != null) cb.onAction(msg);
            }
            @Override public void onError(SmartDevice dev, String err) {
                String msg = "Impossible d'éteindre " + dev.friendlyName;
                voice.speak(msg);
                if (cb != null) cb.onAction(msg);
            }
        });
    }

    private void executeToggle(SmartDevice d, VoiceResponse cb) {
        control.toggle(d, new DeviceControlManager.ControlCallback() {
            @Override public void onSuccess(SmartDevice dev, String state) {
                String msg = dev.friendlyName + " " + state;
                voice.speak(msg);
                if (cb != null) cb.onAction(msg);
            }
            @Override public void onError(SmartDevice dev, String err) {
                if (cb != null) cb.onAction("Erreur bascule : " + err);
            }
        });
    }

    private void executeBrightness(SmartDevice d, int pct, VoiceResponse cb) {
        control.setBrightness(d, pct, new DeviceControlManager.ControlCallback() {
            @Override public void onSuccess(SmartDevice dev, String state) {
                String msg = "Luminosité réglée à " + pct + "%";
                voice.speak(msg);
                if (cb != null) cb.onAction(msg);
            }
            @Override public void onError(SmartDevice dev, String err) {
                if (cb != null) cb.onAction("Erreur luminosité : " + err);
            }
        });
    }

    private void executeTemperature(SmartDevice d, float temp, VoiceResponse cb) {
        control.setTemperature(d, temp, new DeviceControlManager.ControlCallback() {
            @Override public void onSuccess(SmartDevice dev, String state) {
                String msg = "Température réglée à " + temp + "°";
                voice.speak(msg);
                if (cb != null) cb.onAction(msg);
            }
            @Override public void onError(SmartDevice dev, String err) {
                if (cb != null) cb.onAction("Erreur température : " + err);
            }
        });
    }

    private void executeScene(String sceneName, VoiceResponse cb) {
        ha.activateScene("scene." + sceneName.replace(" ", "_"),
            new HomeAssistantController.HaCallback() {
                @Override public void onSuccess(String r) {
                    String msg = "Scène " + sceneName + " activée";
                    voice.speak(msg);
                    if (cb != null) cb.onAction(msg);
                }
                @Override public void onError(String e) {
                    voice.speak("Scène introuvable");
                    if (cb != null) cb.onAction("Scène introuvable : " + sceneName);
                }
            });
    }

    private void noDevice(String command, VoiceResponse cb) {
        voice.speak("Je n'ai pas trouvé cet appareil");
        if (cb != null) cb.onSpeak("Aucun appareil trouvé pour : " + command);
    }
}
