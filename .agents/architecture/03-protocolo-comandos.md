# 03. Protocolo de Comandos por Correo Electrónico

Este documento define la interfaz de comandos enviada en el encabezado `Subject` del correo electrónico para el buzón `grupo02sa@tecnoweb.org.bo`.

---

## 1. Convención General de Sintaxis

* **Estructura del Asunto (`Subject`):**
  ```text
  COMANDO["parametro1", "parametro2", ..., "parametroN"]
  ```
* **Listado General / Sin parámetros específicos:**
  ```text
  COMANDO["*"]  o  COMANDO[""]
  ```
* **Manejo de Parámetros:**
  * Parámetros separados por comas y delimitados por comillas dobles.
  * Los parámetros de texto preservan espacios y puntuación.
* **Formato de Respuesta:**
  * **Operaciones de Mutación (Insert, Update, Delete):** Respuesta en texto plano informando éxito con identificador generado o detalle del error.
  * **Listados, Malla, Grafo, Dashboard y Reportes:** Respuesta en **HTML estructurado con tablas y estilos en línea** (`style="..."`).

---

## 2. Catálogo de Comandos por Caso de Uso

### CU1. Gestión de Usuarios y Roles (con Foto)
| Comando | Parámetros | Descripción | Formato Respuesta |
| :--- | :--- | :--- | :--- |
| `LISUSU["*"]` | `*` | Lista todos los usuarios registrados con su rol y estado. | Tabla HTML |
| `INSUSU[...]` | `"nombre_completo", "email", "username", "pass", "id_rol", "foto_ruta"` | Registra un nuevo usuario en el sistema. | Texto plano con ID |
| `UPDUSU[...]` | `"id", "nombre_completo", "email", "username", "pass", "id_rol", "foto_ruta", "activo"` | Actualiza los datos de un usuario existente. | Texto plano |
| `DELUSU[...]` | `"id"` | Realiza baja lógica/física del usuario. | Texto plano |

### CU2. Gestión de Estructura de Plan de Estudios y Asignaturas
| Comando | Parámetros | Descripción | Formato Respuesta |
| :--- | :--- | :--- | :--- |
| `LISPLA["*"]` | `*` | Lista los planes de estudio registrados. | Tabla HTML |
| `INSPLA[...]` | `"codigo", "carrera", "anio", "descripcion", "objetivos", "perfil_egreso", "min_cred", "max_cred"` | Crea un nuevo plan curricular (RF-01). | Texto plano con ID |
| `UPDPLA[...]` | `"id", "codigo", "carrera", "anio", "estado"` | Modifica datos generales o estado del plan. | Texto plano |
| `LISASIG[...]` | `"id_plan"` | Lista todas las asignaturas de un plan por semestre (RF-02). | Tabla HTML |
| `INSASIG[...]` | `"sigla", "nombre", "semestre", "ht", "hp", "creditos", "area_formativa", "id_plan"` | Registra una nueva asignatura y calcula créditos. | Texto plano con ID |
| `UPDASIG[...]` | `"id", "sigla", "nombre", "semestre", "ht", "hp", "creditos", "area_formativa"` | Actualiza los datos de una materia. | Texto plano |
| `DELASIG[...]` | `"id"` | Elimina una materia si no tiene dependencias. | Texto plano |

### CU3. Visualizar Grafo de Prerrequisitos
| Comando | Parámetros | Descripción | Formato Respuesta |
| :--- | :--- | :--- | :--- |
| `INSPRE[...]` | `"id_asignatura", "id_asignatura_requisito"` | Vincula una prelación validando que no genere ciclos en el grafo (RF-03). | Texto plano |
| `DELPRE[...]` | `"id_asignatura", "id_asignatura_requisito"` | Elimina una regla de prerrequisito. | Texto plano |
| `LISPRE[...]` | `"id_asignatura"` | Lista los requisitos directos de una materia. | Tabla HTML |
| `VERGRAFO[...]`| `"id_asignatura"` | Muestra la cadena visual completa: prerrequisitos hacia atrás y materias dependientes hacia adelante (RF-04). | Tabla HTML con árbol de dependencias |

### CU4. Organización del Rediseño y Asignación de Capítulos
| Comando | Parámetros | Descripción | Formato Respuesta |
| :--- | :--- | :--- | :--- |
| `LISCOM[...]` | `"id_plan"` | Lista los componentes temáticos del plan de estudios (RF-05). | Tabla HTML |
| `INSCOM[...]` | `"numero", "nombre", "descripcion", "id_plan"` | Registra un nuevo componente estructural. | Texto plano con ID |
| `LISCAP[...]` | `"id_componente"` | Lista los capítulos ordenados correlativamente. | Tabla HTML |
| `INSCAP[...]` | `"numero", "titulo", "contenido", "id_componente"` | Registra un capítulo en estado inicial 'Borrador'. | Texto plano con ID |
| `ASIGCAP[...]` | `"id_capitulo", "id_usuario"` | Asigna un delegado/redactor responsable al capítulo. | Texto plano |
| `DELASIGCAP[...]`| `"id_capitulo", "id_usuario"` | Remueve la asignación de un redactor. | Texto plano |

### CU5. Control de Estados, Observaciones y Auditoría
| Comando | Parámetros | Descripción | Formato Respuesta |
| :--- | :--- | :--- | :--- |
| `CAMBESTADO[...]` | `"id_capitulo", "nuevo_estado", "justificacion_tecnica", "id_usuario"` | Ejecuta la transición de estado validando reglas (RF-06, RF-07, RF-10). | Texto plano |
| `INSOBS[...]` | `"id_capitulo", "comentario", "id_usuario_autoridad"` | La autoridad registra una observación técnica (RF-08). | Texto plano con ID |
| `LISOBS[...]` | `"id_capitulo"` | Lista todas las observaciones del capítulo y su estado de subsanación. | Tabla HTML |
| `SUBSANOBS[...]` | `"id_observacion", "respuesta_subsanacion", "id_usuario_delegado"` | El redactor responde y subsana la observación. | Texto plano |
| `LISHIST[...]` | `"id_capitulo"` | Muestra la pista de auditoría histórica inmutable del capítulo (RF-10). | Tabla HTML |

### CU6. Repositorio de Evidencias y Búsqueda Facetada
| Comando | Parámetros | Descripción | Formato Respuesta |
| :--- | :--- | :--- | :--- |
| `INSDOC[...]` | `"codigo_norma", "titulo", "tipo_doc", "organo_emisor", "fecha_emision", "archivo_ruta", "id_doc_padre"` | Registra un documento de respaldo o resolución (RF-11, RF-12). | Texto plano con ID |
| `LISDOC["*"]` | `*` | Lista general del catálogo normativo y evidencias. | Tabla HTML |
| `VINCULEVI[...]`| `"id_capitulo", "id_documento_evidencia"` | Adjunta formalmente una evidencia a un capítulo (RF-13). | Texto plano |
| `BUSDOC[...]` | `"criterio", "valor"` | Búsqueda facetada por número, tipo, emisor o rango de fechas (RF-14). | Tabla HTML |

### CU7. Monitorear Avance del Rediseño y Compilar Reporte Final
| Comando | Parámetros | Descripción | Formato Respuesta |
| :--- | :--- | :--- | :--- |
| `VERDASH["*"]` | `*` | Tablero de control con métricas de avance global (%), capítulos por estado y alertas de observados (RF-09). | Dashboard en Tabla HTML |
| `REPORTEFINAL[...]` | `"id_plan"` | Compilador consolidado oficial: estructura de la malla, textos de capítulos aprobados y anexos de normas vigentes (RF-15). | Documento consolidado en HTML |

### CU8. Reportes, Estadísticas y Ayuda
| Comando | Parámetros | Descripción | Formato Respuesta |
| :--- | :--- | :--- | :--- |
| `REPAVANCE["*"]` | `*` | Estadísticas detalladas de cumplimiento por componente y responsable. | Tabla HTML |
| `REPUSUARIOS["*"]`| `*` | Reporte de productividad y revisiones por usuario. | Tabla HTML |
| `HELP["*"]` | `*` | Manual completo de comandos del sistema con ejemplos prácticos. | Tabla HTML estilizada |

---

## 3. Ejemplo de Intercambio de Correo

### Solicitud del Usuario:
```text
De: luifer.camacho@gmail.com
Para: grupo02sa@tecnoweb.org.bo
Asunto: INSCAP["1", "Marco Referencial e Institucional", "Contenido del capitulo...", "1"]
```

### Respuesta del Servidor:
```text
De: grupo02sa@tecnoweb.org.bo
Para: luifer.camacho@gmail.com
Asunto: RE: INSCAP["1", "Marco Referencial e Institucional", "Contenido del capitulo...", "1"]

Capítulo registrado correctamente en estado 'Borrador'. Tu ID de Capítulo es: 1
```
