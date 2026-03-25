# KisolTec WebView App

Aplicativo Android simples que abre o webapp KisolTec em uma WebView.

## Requisitos

- Android 5.0 (API 21) ou superior
- Conexão com internet

## Como compilar

### Usando Android Studio
1. Abra o projeto no Android Studio
2. Aguarde a sincronização do Gradle
3. Build > Build Bundle(s) / APK(s) > Build APK(s)

### Usando linha de comando
```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

O APK será gerado em `app/build/outputs/apk/`

## Tratativas para Android 5 Samsung

Este aplicativo inclui as seguintes tratativas para compatibilidade com Samsung Android 5:

1. **CookieSyncManager**: Inicialização manual para evitar crashes
2. **SSL**: Tratamento especial para erros de SSL em dispositivos antigos
3. **WebViewClient**: Implementação robusta para lidar com erros de carregamento
4. **Timeout**: Timeout de 30 segundos para evitar carregamento infinito
5. **Network Security Config**: Configurado para permitir conexões HTTP/HTTPS

## Estrutura do Projeto

```
app/
├── src/main/
│   ├── java/com/kisoltec/webviewapp/
│   │   └── MainActivity.java
│   ├── res/
│   │   ├── layout/
│   │   │   └── activity_main.xml
│   │   ├── values/
│   │   │   ├── strings.xml
│   │   │   ├── colors.xml
│   │   │   └── styles.xml
│   │   ├── xml/
│   │   │   └── network_security_config.xml
│   │   └── mipmap-*/
│   │       └── ic_launcher*.xml
│   └── AndroidManifest.xml
└── build.gradle
```

## URL do WebApp

O aplicativo carrega automaticamente: https://kisoltec-producao-webapp.web.app/
