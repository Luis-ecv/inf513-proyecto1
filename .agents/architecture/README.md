# Arquitectura del Sistema: Seguimiento al Diseño Curricular

Este directorio centraliza la documentación arquitectónica, diseño de datos y protocolo de comandos para el proyecto **email-P** (Grupo 02 SA - TecnoWeb).

---

## Documentación del Sistema

1. **[01-diagrama-clases.md](file:///l:/projects%202-2026/TW/email-P/.agents/architecture/01-diagrama-clases.md):**
   * Diagrama de clases oficial en PlantUML.
   * Diccionario de clases, atributos, cardinalidades y cobertura de los Requisitos Funcionales (RF-01 a RF-15).

2. **[02-diseno-base-datos.md](file:///l:/projects%202-2026/TW/email-P/.agents/architecture/02-diseno-base-datos.md):**
   * Script DDL completo en SQL para PostgreSQL (`db_grupo02sa` en `db.tecnoweb.org.bo`).
   * 14 tablas relacionales con restricciones de integridad referencial, checks, índices para búsqueda facetada y datos semilla (seeds).

3. **[03-protocolo-comandos.md](file:///l:/projects%202-2026/TW/email-P/.agents/architecture/03-protocolo-comandos.md):**
   * Catálogo integral de comandos para los 8 Casos de Uso (CU1 a CU8).
   * Sintaxis en el encabezado `Subject` (`COMANDO["param1", "param2", ...]`) y formato de respuesta (texto plano vs tablas HTML estilizadas).
