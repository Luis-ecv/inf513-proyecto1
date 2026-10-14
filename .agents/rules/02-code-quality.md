# Calidad de Código y Prevención de Hardcodeo

## 1. Estrategia Anti-Hardcodeo (3 Niveles)

Queda estrictamente prohibido introducir valores mágicos (magic strings, magic numbers) directamente en la lógica de negocio. Se debe emplear la siguiente jerarquía:

### Nivel 1: Variables de Entorno (`.env`)
- **Propósito:** Secretos, credenciales, puertos, URLs de APIs/servicios y configuración sensible o dependiente del entorno (desarrollo, staging, producción).
- **Regla:** Incluir siempre un archivo `.env.example` actualizado con nombres de variables y valores de referencia no sensibles.
- **Acceso:** Centralizar el acceso a variables de entorno a través de un módulo de configuración con validación de tipos/esquema (ej. Zod o validadores similares).

### Nivel 2: Configuración de Negocio (`config/`)
- **Propósito:** Parámetros configurables que no son secretos (límites de paginación, tiempos de expiración de cache, reintentos de red, feature flags).
- **Regla:** Centralizar en archivos de configuración específicos y tipados, nunca duplicados en componentes o controladores.

### Nivel 3: Constantes de Dominio (`constants/` / `enums`)
- **Propósito:** Valores fijos propios del modelo del dominio (estados de una orden, roles de usuario, tipos de eventos).
- **Regla:** Declarar como enums o constantes inmutables (`as const` en TypeScript / enums en Python).

## 2. Estándares Generales de Desarrollo
- **Tipado Estricto:** Prohibido el uso indiscriminado de `any`. Toda estructura de datos debe contar con interfaces o tipos explícitos.
- **Manejo Robusto de Errores:** No silenciar excepciones en bloques catch vacíos. Implementar logging contextual y respuestas controladas.
- **Modularidad y Responsabilidad Única (SRP):** Mantener funciones y componentes con propósitos claros y acotados.
