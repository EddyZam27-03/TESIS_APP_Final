# Ensenando - Aplicación de Enseñanza de Lengua de Señas ULEAM

Aplicación Android completa para la enseñanza adaptativa de lengua de señas desarrollada para la Universidad Laica Eloy Alfaro de Manabí (ULEAM).

## Características Principales

- ✅ **Offline-First**: Funciona sin conexión, sincroniza cuando hay internet
- ✅ **Reconocimiento de Gestos**: Integración con TensorFlow Lite para reconocimiento en tiempo real
- ✅ **Sistema de Roles**: Estudiante, Docente y Administrador con permisos jerárquicos
- ✅ **Sincronización Bidireccional**: Sincronización automática con servidor MySQL
- ✅ **Progreso Incremental**: Barra de progreso que solo incrementa, nunca decrece
- ✅ **Reportes**: Generación de reportes PDF/CSV del progreso del usuario
- ✅ **Gestión de Relaciones**: Sistema de solicitudes docente-estudiante

## Tecnologías Utilizadas

### Android
- **Kotlin** + **XML** (NO Jetpack Compose)
- **ViewBinding** obligatorio
- **Room Database** para almacenamiento local
- **Retrofit** para comunicación con API
- **WorkManager** para sincronización en background
- **Navigation Component** para navegación
- **TensorFlow Lite** para reconocimiento de gestos
- **CameraX** para captura de video
- **Material Design 3** con colores institucionales ULEAM

### Backend
- **PHP** con MySQL
- **JWT** para autenticación
- **RESTful API** con respuestas JSON

## Estructura del Proyecto

```
Ensenando/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/ensenando/
│   │   │   ├── data/              # Capa de datos
│   │   │   │   ├── local/         # Room Database
│   │   │   │   └── remote/        # Retrofit API
│   │   │   ├── ui/                # Interfaz de usuario
│   │   │   │   ├── auth/          # Autenticación
│   │   │   │   ├── home/          # Pantalla principal
│   │   │   │   ├── activity/      # Actividades de gestos
│   │   │   │   ├── camera/        # Cámara para reconocimiento
│   │   │   │   ├── profile/       # Perfil del usuario
│   │   │   │   └── welcome/       # Pantalla de bienvenida
│   │   │   ├── ml/                # TensorFlow Lite
│   │   │   ├── work/              # WorkManager
│   │   │   └── util/              # Utilidades
│   │   ├── res/
│   │   │   ├── layout/            # Layouts XML
│   │   │   ├── navigation/       # Navigation Component
│   │   │   ├── menu/             # Menús
│   │   │   └── values/           # Recursos (colores, strings, etc.)
│   │   └── INFO/
│   │       ├── lengua_senas/     # Backend PHP
│   │       └── modelo_lsp.tflite # Modelo TensorFlow Lite
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── MANUAL_INSTALACION.md
└── README.md
```

## Instalación Rápida

### Backend
1. Copiar `app/src/main/INFO/lengua_senas/` al servidor web
2. Configurar `config.php` con credenciales de BD
3. Ejecutar script SQL para crear la base de datos

### Android
1. Abrir proyecto en Android Studio
2. Sincronizar dependencias (Gradle)
3. Configurar URL del API en `RetrofitClient.kt`
4. Compilar y ejecutar

Ver `MANUAL_INSTALACION.md` para instrucciones detalladas.

## Base de Datos

La aplicación utiliza una base de datos MySQL con las siguientes tablas:
- `usuarios`: Usuarios del sistema
- `gestos`: Catálogo de gestos
- `usuario_gestos`: Progreso de usuarios en gestos
- `docenteestudiante`: Relaciones docente-estudiante
- `logros`: Sistema de logros
- `usuario_logros`: Logros obtenidos por usuarios

**Importante**: Los campos `sync_status` y `last_updated` existen SOLO en Room (local), NO en MySQL.

## Sincronización

La aplicación implementa sincronización offline-first:
1. **Push**: Envía cambios locales pendientes al servidor
2. **Pull**: Obtiene actualizaciones del servidor
3. **Resolución de Conflictos**: El cliente tiene prioridad si `last_updated` es mayor

## Sistema de Roles

### Estudiante (Base)
- Ver módulos, submódulos y actividades
- Practicar gestos con reconocimiento
- Ver y gestionar solicitudes de docentes
- Descargar su propio reporte

### Docente (Hereda Estudiante)
- Pedir reportes a estudiantes
- Ver reportes de estudiantes que lo hayan aceptado
- Ver progreso detallado individual y grupal

### Administrador (Hereda Docente)
- Ver todos los reportes sin permiso
- Restablecer actividades de cualquier usuario
- Eliminar relaciones Docente-Estudiante
- Supervisión completa

## Reconocimiento de Gestos

El sistema de reconocimiento utiliza:
- **TensorFlow Lite** para clasificación
- **Detección de manos** (requiere modelo adicional)
- **Validación de precisión** con umbral configurable (default: 0.80)
- **Frames consecutivos** requeridos (default: 5)
- **Progreso incremental** que solo aumenta

## Colores Institucionales ULEAM

- **Primary**: #1E88E5 (Azul)
- **Secondary**: #FF6F00 (Naranja)
- Colores de soporte neutros y suaves

## Documentación

- `MANUAL_INSTALACION.md`: Guía completa de instalación
- `app/src/main/INFO/lengua_senas/README.md`: Documentación del API

## Licencia

Proyecto desarrollado para ULEAM.

## Contacto

Para soporte técnico o consultas, contactar al equipo de desarrollo.


"# TESIS_APP_Final" 
"# TESIS_APP_Final" 
