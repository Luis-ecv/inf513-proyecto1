# Habilidades (Skills) del Proyecto

Este directorio almacena skills especializados y workflows progresivos para este proyecto.

## Estructura de un Skill

Cada skill debe residir en su propia carpeta con un archivo `SKILL.md`:

```
.agents/skills/<nombre-del-skill>/
├── SKILL.md             # Archivo obligatorio con frontmatter YAML (name, description) e instrucciones
├── scripts/              # (Opcional) Scripts utilitarios o de automatización
└── examples/             # (Opcional) Ejemplos o plantillas de referencia
```

## Formato de `SKILL.md`

```markdown
---
name: nombre-del-skill
description: Breve descripción de qué hace el skill y cuándo debe activarse.
---

# Título del Skill

Instrucciones detalladas y procedimientos paso a paso.
```
