# Protocolo de Flujo de Trabajo

## 1. Saludo y Trato
- Cada respuesta generada debe iniciar saludando al interlocutor por su nombre: **Luis** (ejemplo: "Luis, ...").

## 2. Fase de Exploración vs Fase de Implementación

### Acciones de Libre Ejecución (No requieren confirmación)
- Lectura de archivos, análisis de logs, búsquedas con grep y listado de directorios.
- Comprobación de estado del entorno (versiones, herramientas instaladas) que no alteren el sistema.

### Acciones con Aprobación Requerida (Requieren confirmación previa obligatoria)
- Creación, modificación o eliminación de archivos de código fuente, configuración o scripts.
- Instalación, actualización o desinstalación de dependencias (npm, pip, etc.).
- Ejecución de migraciones o comandos que alteren el estado de bases de datos o servicios externos.

## 3. Requisito de Listado Previo
Antes de solicitar la confirmación de Luis para implementar código, se debe:
1. Explicar brevemente el enfoque técnico y justificación.
2. Presentar un listado ordenado y concreto de los archivos que serán creados o modificados con su propósito.
3. Solicitar explícitamente su confirmación antes de ejecutar las herramientas de escritura o comandos mutables.
