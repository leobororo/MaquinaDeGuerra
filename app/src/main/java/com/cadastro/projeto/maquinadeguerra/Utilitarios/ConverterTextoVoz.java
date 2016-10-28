package com.cadastro.projeto.maquinadeguerra.Utilitarios;

import android.os.Build;
import android.speech.tts.TextToSpeech;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

/**
 * Created by Leonardo on 19/10/2016 .
 */
public class ConverterTextoVoz {
    public static void texto(String texto, TextToSpeech mTts, boolean voiceControlActive){
        if (voiceControlActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mTts.speak(texto, QUEUE_FLUSH, null, null);
            } else {
                mTts.speak(texto, QUEUE_FLUSH, null);
            }
        }
    }
}
