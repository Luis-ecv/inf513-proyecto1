package com.curriculo.comandos;

import com.curriculo.dao.AsignaturaDAO;
import com.curriculo.dao.CapituloDAO;
import com.curriculo.dao.ComponenteDAO;
import com.curriculo.dao.DocumentoEvidenciaDAO;
import com.curriculo.dao.HistorialDAO;
import com.curriculo.dao.ObservacionDAO;
import com.curriculo.dao.PlanEstudioDAO;
import com.curriculo.dao.PrerrequisitoDAO;
import com.curriculo.dao.ReporteDAO;
import com.curriculo.dao.UsuarioDAO;
import com.curriculo.modelo.Asignatura;
import com.curriculo.modelo.Capitulo;
import com.curriculo.modelo.Componente;
import com.curriculo.modelo.DocumentoEvidencia;
import com.curriculo.modelo.HistorialModificacion;
import com.curriculo.modelo.Observacion;
import com.curriculo.modelo.PlanEstudio;
import com.curriculo.config.Config;
import com.curriculo.email.EmailSender;
import com.curriculo.modelo.Prerrequisito;
import com.curriculo.modelo.Usuario;
import com.curriculo.seguridad.PasswordHasher;
import com.curriculo.seguridad.PasswordValidator;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Procesador central de comandos recibidos en el Subject del correo.
 * Soporta la totalidad de los 8 Casos de Uso (CU1 al CU8) y los 15 Requisitos
 * Funcionales (RF-01 a RF-15).
 */
public class CommandProcessor {

    private static final Pattern PARAM_PATTERN = Pattern
            .compile("\\s*\"([^\"]*)\"\\s*|\\s*'([^']*)'\\s*|\\s*([^,\\[\\]]+)\\s*");

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final PlanEstudioDAO planEstudioDAO = new PlanEstudioDAO();
    private final AsignaturaDAO asignaturaDAO = new AsignaturaDAO();
    private final PrerrequisitoDAO prerrequisitoDAO = new PrerrequisitoDAO();
    private final ComponenteDAO componenteDAO = new ComponenteDAO();
    private final CapituloDAO capituloDAO = new CapituloDAO();
    private final ObservacionDAO observacionDAO = new ObservacionDAO();
    private final HistorialDAO historialDAO = new HistorialDAO();
    private final DocumentoEvidenciaDAO documentoDAO = new DocumentoEvidenciaDAO();
    private final ReporteDAO reporteDAO = new ReporteDAO();

    public String procesar(String subject, String remitente) {
        if (subject == null || subject.trim().isEmpty()) {
            return "Comando vacío o no reconocido. Envíe HELP[\"*\"] para ver el manual de comandos.";
        }

        String trimmed = subject.trim();
        String commandUpper = trimmed.toUpperCase();

        try {
            // Manual universal de comandos
            if (commandUpper.startsWith("HELP")) {
                return generarManualHTML();
            }

            // Inicio de sesión y vinculación de buzón por correo (exclusivo por email)
            if (commandUpper.startsWith("LOGIN")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en LOGIN", false,
                            "Se esperaban 2 parámetros obligatorios (correo electrónico y contraseña).",
                            new String[][] {
                                    { "Formato Requerido", "LOGIN[\"email\", \"password\"]" },
                                    { "Ejemplo Válido", "LOGIN[\"admin@uagrm.edu.bo\", \"" + PasswordValidator.EJEMPLO_VALIDO + "\"]" }
                            }, "HELP[\"*\"]");
                }
                String email = p[0].trim();
                String pass = p[1].trim();

                Usuario u = usuarioDAO.autenticar(email, pass);
                if (u == null) {
                    return generarTarjetaRespuestaHTML("Acceso Denegado", false,
                            "Credenciales incorrectas o usuario inactivo en el sistema.",
                            new String[][] {
                                    { "Correo Verificado", email },
                                    { "Sugerencia", "Verifique su correo y contraseña e intente nuevamente." }
                            }, "LOGIN[\"email\", \"password\"]");
                }

                return generarPerfilUsuarioHTML(u);
            }

            // Verificación de autorización por rol según la cuenta del remitente
            String errorPermiso = verificarPermisos(commandUpper, remitente);
            if (errorPermiso != null) {
                return errorPermiso;
            }

            // =========================================================================
            // CU1: GESTIÓN DE USUARIOS
            // =========================================================================
            if (commandUpper.startsWith("LISUSU")) {
                List<Usuario> usuarios = usuarioDAO.listar();
                if (usuarios.isEmpty()) {
                    return "No existen usuarios registrados en el sistema.";
                }
                return generarTablaUsuariosHTML(usuarios);
            }

            if (commandUpper.startsWith("INSUSU")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 5) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en INSUSU", false,
                            "Se esperaban al menos 5 parámetros obligatorios para registrar un nuevo usuario.",
                            new String[][] {
                                    { "Formato Requerido", "INSUSU[\"Nombre Completo\", \"email\", \"username\", \"password\", \"idRol\", \"fotoRuta\"]" },
                                    { "Roles Permitidos", "1 = Director, 2 = Delegado Curricular, 3 = Docente Redactor" },
                                    { "Ejemplo Válido", "INSUSU[\"Lic. Carlos Roca\", \"croca@uagrm.edu.bo\", \"croca\", \"" + PasswordValidator.EJEMPLO_VALIDO + "\", \"3\", \"fotos/croca.png\"]" }
                            }, "HELP[\"*\"]", GUIA_FOTO_USUARIO);
                }
                String nombre = p[0].trim();
                String email = p[1].trim();
                String username = p[2].trim();
                String pass = p[3].trim();
                int idRol = parseEntero(p[4], "idRol (1=Director, 2=Delegado, 3=Invitado)");
                String foto = p.length >= 6 && !p[5].trim().isEmpty() ? p[5].trim() : "fotos/default.png";

                // Validación estricta de contraseña
                PasswordValidator.Resultado resPass = PasswordValidator.validar(pass);
                if (!resPass.isValido()) {
                    return generarTarjetaRespuestaHTML("Contraseña Inválida (Seguridad Requerida)", false,
                            resPass.getMensaje(),
                            new String[][] {
                                    { "Requisitos", "Mínimo 8 caracteres, al menos 1 mayúscula, 1 minúscula, 1 número y 1 símbolo especial" },
                                    { "Ejemplo Sugerido", PasswordValidator.EJEMPLO_VALIDO }
                            }, null);
                }

                if (usuarioDAO.obtenerPorEmail(email) != null) {
                    return generarTarjetaRespuestaHTML("Registro Rechazado", false,
                            "La dirección de correo electrónico '" + email + "' ya se encuentra registrada en el sistema.",
                            new String[][] { { "Email en Conflicto", email } }, "LISUSU");
                }
                if (usuarioDAO.obtenerPorUsername(username) != null) {
                    return generarTarjetaRespuestaHTML("Registro Rechazado", false,
                            "El nombre de usuario '" + username + "' ya está en uso. Elija otro nombre de usuario.",
                            new String[][] { { "Usuario en Conflicto", username } }, "LISUSU");
                }

                String passHash = PasswordHasher.hash(pass);
                Usuario u = new Usuario(nombre, email, username, passHash, foto, idRol);
                int idGen = usuarioDAO.insertar(u);
                if (idGen > 0) {
                    String rolNombre = idRol == 1 ? "Director de Carrera" : (idRol == 2 ? "Delegado Curricular / Autoridad" : "Docente Redactor / Invitado");
                    // Enviar notificación formal con credenciales al correo del nuevo usuario en segundo plano
                    final String emailDestino = email;
                    final String htmlNotif = generarNotificacionCredencialesHTML(nombre, email, username, pass, idRol);
                    new Thread(() -> {
                        try {
                            EmailSender.enviarHTML(emailDestino, "[Seguimiento Curricular UAGRM] Sus Credenciales de Acceso", htmlNotif);
                        } catch (Exception ex) {
                            System.err.println("Error al despachar notificación de credenciales a " + emailDestino + ": " + ex.getMessage());
                        }
                    }).start();

                    return generarTarjetaRespuestaHTML("Usuario Registrado Exitosamente", true,
                            "El nuevo usuario ha sido dado de alta en la base de datos y se ha enviado formalmente el correo institucional con sus credenciales de acceso.",
                            new String[][] {
                                    { "ID Asignado", String.valueOf(idGen) },
                                    { "Nombre Completo", nombre },
                                    { "Nombre de Usuario", username },
                                    { "Correo Electrónico", email },
                                    { "Rol Asignado", rolNombre + " (ID: " + idRol + ")" },
                                    { "Notificación Despachada", "Enviada exitosamente a " + email }
                            }, "LISUSU");
                } else {
                    return generarTarjetaRespuestaHTML("Error en Base de Datos", false,
                            "Error al registrar usuario en la base de datos. Verifique que el idRol exista (1=Director, 2=Delegado, 3=Invitado).",
                            null, "LISUSU");
                }
            }

            if (commandUpper.startsWith("UPDUSU")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 7) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en UPDUSU", false,
                            "Se esperaban al menos 7 parámetros para actualizar los datos de un usuario.",
                            new String[][] {
                                    { "Formato Requerido", "UPDUSU[\"id\", \"Nombre Completo\", \"email\", \"username\", \"password\", \"idRol\", \"fotoRuta\", \"activo\"]" },
                                    { "Ejemplo Válido", "UPDUSU[\"1\", \"Carlos Roca\", \"croca@uagrm.edu.bo\", \"croca\", \"" + PasswordValidator.EJEMPLO_VALIDO + "\", \"3\", \"fotos/croca.png\", \"true\"]" }
                            }, "LISUSU", GUIA_FOTO_USUARIO);
                }
                int id = parseEntero(p[0], "id del usuario");
                String nombre = p[1].trim();
                String email = p[2].trim();
                String username = p[3].trim();
                String pass = p[4].trim();
                int idRol = parseEntero(p[5], "idRol (1=Director, 2=Delegado, 3=Invitado)");
                String foto = p.length >= 7 && !p[6].trim().isEmpty() ? p[6].trim() : "fotos/default.png";
                boolean activo = p.length >= 8 ? Boolean.parseBoolean(p[7]) : true;

                // Validación estricta de contraseña en actualización (no se permiten vacíos)
                PasswordValidator.Resultado resPass = PasswordValidator.validar(pass);
                if (!resPass.isValido()) {
                    return generarTarjetaRespuestaHTML("Contraseña Inválida en UPDUSU", false,
                            resPass.getMensaje(), null, null);
                }

                String passHash = PasswordHasher.hash(pass);
                Usuario u = new Usuario(id, nombre, email, username, passHash, foto, activo, null, idRol, null);
                boolean ok = usuarioDAO.actualizar(u);
                if (ok) {
                    return generarTarjetaRespuestaHTML("Usuario Actualizado Exitosamente", true,
                            "La información del usuario ID " + id + " ha sido actualizada en la base de datos.",
                            new String[][] {
                                    { "ID Usuario", String.valueOf(id) },
                                    { "Nombre Completo", nombre },
                                    { "Correo Electrónico", email },
                                    { "Nombre de Usuario", username },
                                    { "Rol Asignado", "Rol " + idRol },
                                    { "Estado", activo ? "Activo" : "Inactivo" }
                            }, "LISUSU");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Actualizar Usuario", false,
                            "No se pudo actualizar el usuario ID " + id + ". Verifique que el ID exista.",
                            null, "LISUSU");
                }
            }

            if (commandUpper.startsWith("DELUSU")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 1) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en DELUSU", false,
                            "Formato esperado: DELUSU[\"id\"]",
                            new String[][] { { "Ejemplo Válido", "DELUSU[\"2\"]" } }, "LISUSU");
                }
                int id = parseEntero(p[0], "id del usuario");
                boolean ok = usuarioDAO.eliminar(id);
                if (ok) {
                    return generarTarjetaRespuestaHTML("Usuario Eliminado / Desactivado", true,
                            "El usuario ID " + id + " ha sido eliminado/desactivado correctamente del sistema.",
                            new String[][] { { "ID Usuario", String.valueOf(id) } }, "LISUSU");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Eliminar Usuario", false,
                            "No se pudo eliminar el usuario ID " + id + ". Verifique que exista y no posea dependencias activas.",
                            null, "LISUSU");
                }
            }

            // =========================================================================
            // CU2: GESTIÓN DE PLAN DE ESTUDIOS
            // =========================================================================
            if (commandUpper.startsWith("LISPLA")) {
                List<PlanEstudio> planes = planEstudioDAO.listar();
                if (planes.isEmpty()) {
                    return "No existen planes de estudio registrados.";
                }
                return generarTablaPlanesHTML(planes);
            }

            if (commandUpper.startsWith("INSPLA")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 8) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en INSPLA", false,
                            "Se esperaban 8 parámetros para registrar el plan de estudios.",
                            new String[][] {
                                    { "Formato Requerido", "INSPLA[\"codigo\", \"carrera\", \"anio\", \"descripcion\", \"objetivos\", \"perfil_egreso\", \"minCreditos\", \"maxCreditos\"]" },
                                    { "Ejemplo Válido", "INSPLA[\"SIS-2026\", \"Ingenieria en Sistemas\", \"2026\", \"Nuevo plan\", \"Formar profesionales\", \"Perfil de egreso\", \"200\", \"240\"]" }
                            }, "LISPLA");
                }
                int anio = parseEntero(p[2], "anio del plan");
                int minCred = parseEntero(p[6], "minCreditos");
                int maxCred = parseEntero(p[7], "maxCreditos");
                PlanEstudio pe = new PlanEstudio(p[0].trim(), p[1].trim(), anio, p[3].trim(), p[4].trim(), p[5].trim(),
                        minCred, maxCred);
                int idGen = planEstudioDAO.insertar(pe);
                if (idGen > 0) {
                    return generarTarjetaRespuestaHTML("Plan de Estudios Registrado Exitosamente", true,
                            "El plan de estudios curricular ha sido registrado en el sistema.",
                            new String[][] {
                                    { "ID Asignado", String.valueOf(idGen) },
                                    { "Código del Plan", p[0].trim() },
                                    { "Carrera", p[1].trim() },
                                    { "Año de Vigencia", String.valueOf(anio) },
                                    { "Rango de Créditos", minCred + " - " + maxCred }
                            }, "LISPLA");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Registrar Plan", false,
                            "No se pudo registrar el plan de estudios. Verifique que el código no esté duplicado en el sistema.",
                            new String[][] { { "Código Intentado", p[0].trim() } }, "LISPLA");
                }
            }

            if (commandUpper.startsWith("UPDPLA")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 10) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en UPDPLA", false,
                            "Se esperaban 10 parámetros para actualizar el plan de estudios.",
                            new String[][] {
                                    { "Formato Requerido", "UPDPLA[\"id\", \"codigo\", \"carrera\", \"anio\", \"descripcion\", \"objetivos\", \"perfil_egreso\", \"minCred\", \"maxCred\", \"estado\"]" },
                                    { "Ejemplo Válido", "UPDPLA[\"1\", \"SIS-2026\", \"Ingenieria en Sistemas\", \"2026\", \"Desc\", \"Obj\", \"Perfil\", \"200\", \"240\", \"En Elaboracion\"]" }
                            }, "LISPLA");
                }
                int id = parseEntero(p[0], "id del plan");
                int anio = parseEntero(p[3], "anio del plan");
                int minCred = parseEntero(p[7], "minCred");
                int maxCred = parseEntero(p[8], "maxCred");
                PlanEstudio pe = new PlanEstudio(id, p[1].trim(), p[2].trim(), anio, p[4].trim(), p[5].trim(),
                        p[6].trim(), minCred, maxCred, p[9].trim(), null);
                boolean ok = planEstudioDAO.actualizar(pe);
                if (ok) {
                    return generarTarjetaRespuestaHTML("Plan de Estudios Actualizado Exitosamente", true,
                            "El plan de estudios ID " + id + " ha sido actualizado en la base de datos.",
                            new String[][] {
                                    { "ID Plan", String.valueOf(id) },
                                    { "Código", p[1].trim() },
                                    { "Carrera", p[2].trim() },
                                    { "Estado", p[9].trim() }
                            }, "LISPLA");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Actualizar Plan", false,
                            "No se pudo actualizar el plan de estudios ID " + id + ". Verifique que el identificador exista.",
                            null, "LISPLA");
                }
            }

            // =========================================================================
            // CU2: GESTIÓN DE ASIGNATURAS
            // =========================================================================
            if (commandUpper.startsWith("LISASIG")) {
                String[] p = extraerParams(trimmed);
                List<Asignatura> materias;
                if (p != null && p.length >= 1 && !p[0].equals("*") && !p[0].isEmpty()) {
                    int idPlan = parseEntero(p[0], "idPlan");
                    materias = asignaturaDAO.listarPorPlan(idPlan);
                } else {
                    materias = asignaturaDAO.listarTodas();
                }

                if (materias.isEmpty()) {
                    return "No existen asignaturas registradas para el criterio solicitado.";
                }
                return generarTablaAsignaturasHTML(materias);
            }

            if (commandUpper.startsWith("INSASIG")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 8) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en INSASIG", false,
                            "Se esperaban 8 parámetros para crear una nueva asignatura.",
                            new String[][] {
                                    { "Formato Requerido", "INSASIG[\"sigla\", \"nombre\", \"semestre\", \"horasTeoricas\", \"horasPracticas\", \"creditos\", \"areaFormativa\", \"idPlanEstudio\"]" },
                                    { "Ejemplo Válido", "INSASIG[\"INF110\", \"Introduccion a la Informatica\", \"1\", \"4\", \"2\", \"5\", \"Ciencias de la Computacion\", \"1\"]" }
                            }, "LISPLA");
                }
                int sem = parseEntero(p[2], "semestre (1 al 10)");
                int ht = parseEntero(p[3], "horasTeoricas");
                int hp = parseEntero(p[4], "horasPracticas");
                int cred = parseEntero(p[5], "creditos");
                int idPlan = parseEntero(p[7], "idPlanEstudio");

                Asignatura a = new Asignatura(p[0].trim(), p[1].trim(), sem, ht, hp, cred, p[6].trim(), idPlan);
                int idGen = asignaturaDAO.insertar(a);
                if (idGen > 0) {
                    return generarTarjetaRespuestaHTML("Asignatura Registrada Exitosamente", true,
                            "La asignatura curricular ha sido vinculada al plan de estudios.",
                            new String[][] {
                                    { "ID Asignatura", String.valueOf(idGen) },
                                    { "Sigla", p[0].trim() },
                                    { "Nombre", p[1].trim() },
                                    { "Semestre", String.valueOf(sem) },
                                    { "Carga Horaria (HT/HP)", ht + " hrs / " + hp + " hrs" },
                                    { "Créditos", String.valueOf(cred) },
                                    { "Área Formativa", p[6].trim() },
                                    { "Plan de Estudios ID", String.valueOf(idPlan) }
                            }, "LISASIG[\"" + idPlan + "\"]");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Registrar Asignatura", false,
                            "Error al insertar en base de datos. Verifique que la sigla sea única en el plan y que el idPlan exista.",
                            new String[][] { { "Sigla Intentada", p[0].trim() } }, "LISPLA");
                }
            }

            if (commandUpper.startsWith("UPDASIG")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 8) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en UPDASIG", false,
                            "Se esperaban 8 parámetros para actualizar la asignatura.",
                            new String[][] {
                                    { "Formato Requerido", "UPDASIG[\"id\", \"sigla\", \"nombre\", \"semestre\", \"horasTeoricas\", \"horasPracticas\", \"creditos\", \"areaFormativa\"]" },
                                    { "Ejemplo Válido", "UPDASIG[\"1\", \"INF110\", \"Introduccion a la Informatica\", \"1\", \"4\", \"2\", \"5\", \"Ciencias de la Computacion\"]" }
                            }, "LISASIG");
                }
                int id = parseEntero(p[0], "id de la asignatura");
                int sem = parseEntero(p[3], "semestre");
                int ht = parseEntero(p[4], "horasTeoricas");
                int hp = parseEntero(p[5], "horasPracticas");
                int cred = parseEntero(p[6], "creditos");

                Asignatura a = new Asignatura(id, p[1].trim(), p[2].trim(), sem, ht, hp, cred, p[7].trim(), 0);
                boolean ok = asignaturaDAO.actualizar(a);
                if (ok) {
                    return generarTarjetaRespuestaHTML("Asignatura Actualizada Exitosamente", true,
                            "Los datos de la asignatura ID " + id + " han sido actualizados en la base de datos.",
                            new String[][] {
                                    { "ID Asignatura", String.valueOf(id) },
                                    { "Sigla", p[1].trim() },
                                    { "Nombre", p[2].trim() },
                                    { "Semestre", String.valueOf(sem) },
                                    { "Créditos", String.valueOf(cred) }
                            }, "LISASIG");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Actualizar Asignatura", false,
                            "No se pudo actualizar la asignatura ID " + id + ". Verifique que el ID exista.",
                            null, "LISASIG");
                }
            }

            if (commandUpper.startsWith("DELASIG")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 1) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en DELASIG", false,
                            "Formato esperado: DELASIG[\"id\"]",
                            new String[][] { { "Ejemplo Válido", "DELASIG[\"1\"]" } }, "LISASIG");
                }
                int id = parseEntero(p[0], "id de la asignatura");
                boolean ok = asignaturaDAO.eliminar(id);
                if (ok) {
                    return generarTarjetaRespuestaHTML("Asignatura Eliminada Exitosamente", true,
                            "La asignatura ID " + id + " ha sido retirada de la malla curricular.",
                            new String[][] { { "ID Asignatura", String.valueOf(id) } }, "LISASIG");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Eliminar Asignatura", false,
                            "No se pudo eliminar la asignatura ID " + id + ". Verifique si tiene prerrequisitos o dependencias activas.",
                            null, "LISASIG");
                }
            }

            // =========================================================================
            // CU3: GESTIÓN DE GRAFO DE PRERREQUISITOS (RF-03, RF-04)
            // =========================================================================
            if (commandUpper.startsWith("INSPRE")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en INSPRE", false,
                            "Formato esperado: INSPRE[\"idAsignatura\", \"idAsignaturaRequisito\"]",
                            new String[][] { { "Ejemplo Válido", "INSPRE[\"5\", \"1\"]" } }, "HELP[\"*\"]");
                }
                int idAsig = parseEntero(p[0], "idAsignatura");
                int idReq = parseEntero(p[1], "idAsignaturaRequisito");

                int resultado = prerrequisitoDAO.insertarConValidacion(idAsig, idReq);
                if (resultado == 1) {
                    return generarTarjetaRespuestaHTML("Prerrequisito Registrado Exitosamente", true,
                            "La dependencia curricular ha sido validada e incorporada a la malla.",
                            new String[][] {
                                    { "Asignatura Destino ID", String.valueOf(idAsig) },
                                    { "Prerrequisito Requerido ID", String.valueOf(idReq) },
                                    { "Validación de Grafo", "Aprobada (Sin ciclos detectados - RF-03)" }
                            }, "VERGRAFO[\"" + idAsig + "\"]");
                } else if (resultado == -1) {
                    return generarTarjetaRespuestaHTML("Rechazo por Validación de Grafo (RF-03)", false,
                            "La inserción crearía una dependencia circular (ciclo directo o indirecto) entre las materias.",
                            new String[][] {
                                    { "Asignatura Destino ID", String.valueOf(idAsig) },
                                    { "Prerrequisito Intentado ID", String.valueOf(idReq) },
                                    { "Regla Académica", "La malla curricular debe ser estrictamente un Grafo Acíclico Dirigido (DAG)" }
                            }, "VERGRAFO[\"" + idAsig + "\"]");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Registrar Prerrequisito", false,
                            "Error al registrar la correlatividad. Verifique que ambas asignaturas existan y no se encuentre duplicada.",
                            null, "LISPRE[\"" + idAsig + "\"]");
                }
            }

            if (commandUpper.startsWith("DELPRE")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en DELPRE", false,
                            "Formato esperado: DELPRE[\"idAsignatura\", \"idAsignaturaRequisito\"]",
                            new String[][] { { "Ejemplo Válido", "DELPRE[\"5\", \"1\"]" } }, "HELP[\"*\"]");
                }
                int idAsig = parseEntero(p[0], "idAsignatura");
                int idReq = parseEntero(p[1], "idAsignaturaRequisito");
                boolean ok = prerrequisitoDAO.eliminar(idAsig, idReq);
                if (ok) {
                    return generarTarjetaRespuestaHTML("Prerrequisito Eliminado Exitosamente", true,
                            "La correlatividad entre las materias especificadas ha sido removida.",
                            new String[][] {
                                    { "Asignatura Destino ID", String.valueOf(idAsig) },
                                    { "Prerrequisito Removido ID", String.valueOf(idReq) }
                            }, "VERGRAFO[\"" + idAsig + "\"]");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Eliminar Prerrequisito", false,
                            "No se encontró el prerrequisito especificado para eliminar.",
                            null, "VERGRAFO[\"" + idAsig + "\"]");
                }
            }

            if (commandUpper.startsWith("LISPRE")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 1) {
                    return "Error de sintaxis en LISPRE: Formato esperado: LISPRE[\"idAsignatura\"]\nEjemplo: LISPRE[\"5\"]";
                }
                int idAsig = parseEntero(p[0], "idAsignatura");
                List<Prerrequisito> lista = prerrequisitoDAO.listarRequisitosDirectos(idAsig);
                if (lista.isEmpty()) {
                    return "La asignatura ID " + idAsig + " no tiene prerrequisitos directos registrados.";
                }
                return generarTablaPrerrequisitosDirectosHTML(idAsig, lista);
            }

            if (commandUpper.startsWith("VERGRAFO")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 1) {
                    return "Error de sintaxis en VERGRAFO: Formato esperado: VERGRAFO[\"idAsignatura\"]\nEjemplo: VERGRAFO[\"5\"]";
                }
                int idAsig = parseEntero(p[0], "idAsignatura");
                Asignatura actual = asignaturaDAO.obtenerPorId(idAsig);
                if (actual == null) {
                    return "Error: No existe una asignatura con ID " + idAsig + ".";
                }

                List<Asignatura> haciaAtras = prerrequisitoDAO.obtenerCadenaHaciaAtras(idAsig);
                List<Asignatura> haciaAdelante = prerrequisitoDAO.obtenerCadenaHaciaAdelante(idAsig);

                return generarVistaGrafoHTML(actual, haciaAtras, haciaAdelante);
            }

            // =========================================================================
            // CU4: ESTRUCTURA DEL REDISEÑO (COMPONENTES Y CAPÍTULOS - RF-05)
            // =========================================================================
            if (commandUpper.startsWith("LISCOM")) {
                String[] p = extraerParams(trimmed);
                List<Componente> lista;
                if (p != null && p.length >= 1 && !p[0].equals("*") && !p[0].isEmpty()) {
                    int idPlan = parseEntero(p[0], "idPlan");
                    lista = componenteDAO.listarPorPlan(idPlan);
                } else {
                    lista = componenteDAO.listarTodos();
                }

                if (lista.isEmpty()) {
                    return "No existen componentes registrados para el criterio solicitado.";
                }
                return generarTablaComponentesHTML(lista);
            }

            if (commandUpper.startsWith("INSCOM")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 4) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en INSCOM", false,
                            "Formato esperado: INSCOM[\"numero\", \"nombre\", \"descripcion\", \"idPlanEstudio\"]",
                            new String[][] { { "Ejemplo Válido", "INSCOM[\"1\", \"Marco Referencial\", \"Bases institucionales\", \"1\"]" } }, "LISPLA");
                }
                int num = parseEntero(p[0], "numeroComponente");
                int idPlan = parseEntero(p[3], "idPlanEstudio");
                Componente c = new Componente(num, p[1].trim(), p[2].trim(), idPlan);
                int idGen = componenteDAO.insertar(c);
                if (idGen > 0) {
                    return generarTarjetaRespuestaHTML("Componente Curricular Registrado", true,
                            "El componente curricular ha sido creado exitosamente en la estructura del rediseño.",
                            new String[][] {
                                    { "ID Componente", String.valueOf(idGen) },
                                    { "Número", String.valueOf(num) },
                                    { "Nombre", p[1].trim() },
                                    { "Plan de Estudios ID", String.valueOf(idPlan) }
                            }, "LISCOM[\"" + idPlan + "\"]");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Registrar Componente", false,
                            "Error al registrar componente. Verifique que el número no esté duplicado en el plan y que el idPlan exista.",
                            new String[][] { { "Número Intentado", String.valueOf(num) }, { "Plan ID", String.valueOf(idPlan) } }, "LISPLA");
                }
            }

            if (commandUpper.startsWith("LISCAP")) {
                String[] p = extraerParams(trimmed);
                List<Capitulo> caps;
                if (p != null && p.length >= 1 && !p[0].equals("*") && !p[0].isEmpty()) {
                    int idComp = parseEntero(p[0], "idComponente");
                    caps = capituloDAO.listarPorComponente(idComp);
                } else {
                    caps = capituloDAO.listarTodos();
                }

                if (caps.isEmpty()) {
                    return "No existen capítulos registrados para el criterio solicitado.";
                }
                return generarTablaCapitulosHTML(caps);
            }

            if (commandUpper.startsWith("INSCAP")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 4) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en INSCAP", false,
                            "Se esperaban 4 parámetros obligatorios para registrar un capítulo curricular.",
                            new String[][] {
                                    { "Formato Requerido", "INSCAP[\"numero\", \"titulo\", \"contenido\", \"idComponente\"]" },
                                    { "Ejemplo Válido", "INSCAP[\"1\", \"Diagnóstico Profesional\", \"Estudio del mercado laboral en la región...\", \"1\"]" }
                            }, "HELP[\"*\"]");
                }
                int num = parseEntero(p[0], "numeroCapitulo");
                int idComp = parseEntero(p[3], "idComponente");
                Capitulo cap = new Capitulo(num, p[1].trim(), p[2].trim(), idComp);
                int idGen = capituloDAO.insertar(cap);
                if (idGen > 0) {
                    return generarTarjetaRespuestaHTML("Capítulo Curricular Registrado", true,
                            "El capítulo ha sido incorporado al componente en estado inicial 'Borrador'.",
                            new String[][] {
                                    { "ID Asignado", String.valueOf(idGen) },
                                    { "Número de Capítulo", String.valueOf(num) },
                                    { "Título del Capítulo", p[1].trim() },
                                    { "Componente ID", String.valueOf(idComp) },
                                    { "Estado Inicial", "Borrador" }
                            }, "LISCAP[\"" + idComp + "\"]");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Registrar Capítulo", false,
                            "Error al registrar capítulo en la base de datos. Verifique que el número no esté duplicado en el componente y que el idComponente exista.",
                            new String[][] { { "Número Intentado", String.valueOf(num) }, { "Componente ID", String.valueOf(idComp) } }, "LISCOM");
                }
            }

            if (commandUpper.startsWith("UPDCAP")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 5) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en UPDCAP", false,
                            "Formato esperado: UPDCAP[\"id\", \"numero\", \"titulo\", \"contenido\", \"idComponente\"]",
                            new String[][] { { "Ejemplo Válido", "UPDCAP[\"1\", \"1\", \"Justificacion Curricular Actualizada\", \"Nuevo texto...\", \"1\"]" } }, "LISCAP");
                }
                int id = parseEntero(p[0], "id del capitulo");
                int num = parseEntero(p[1], "numero de capitulo");
                int idComp = parseEntero(p[4], "idComponente");

                Capitulo cap = new Capitulo(id, num, p[2].trim(), p[3].trim(), idComp, 0, null);
                boolean ok = capituloDAO.actualizar(cap);
                if (ok) {
                    return generarTarjetaRespuestaHTML("Capítulo Actualizado Exitosamente", true,
                            "El contenido y los datos del capítulo ID " + id + " han sido modificados en el sistema.",
                            new String[][] {
                                    { "ID Capítulo", String.valueOf(id) },
                                    { "Número", String.valueOf(num) },
                                    { "Título", p[2].trim() },
                                    { "Componente ID", String.valueOf(idComp) }
                            }, "LISCAP[\"" + idComp + "\"]");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Actualizar Capítulo", false,
                            "No se pudo actualizar el capítulo ID " + id + ". Verifique que el ID exista.",
                            null, "LISCAP");
                }
            }

            if (commandUpper.startsWith("DELCAP")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 1) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en DELCAP", false,
                            "Formato esperado: DELCAP[\"id\"]",
                            new String[][] { { "Ejemplo Válido", "DELCAP[\"1\"]" } }, "LISCAP");
                }
                int id = parseEntero(p[0], "id del capitulo");
                boolean ok = capituloDAO.eliminar(id);
                if (ok) {
                    return generarTarjetaRespuestaHTML("Capítulo Eliminado Exitosamente", true,
                            "El capítulo ID " + id + " ha sido eliminado correctamente del componente.",
                            new String[][] { { "ID Capítulo", String.valueOf(id) } }, "LISCAP");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Eliminar Capítulo", false,
                            "No se pudo eliminar el capítulo ID " + id + ". Verifique si tiene dependencias activas (observaciones, evidencias o asignaciones).",
                            null, "LISCAP");
                }
            }

            if (commandUpper.startsWith("ASIGCAP")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en ASIGCAP", false,
                            "Formato esperado: ASIGCAP[\"idCapitulo\", \"idUsuario\"]",
                            new String[][] { { "Ejemplo Válido", "ASIGCAP[\"1\", \"3\"]" } }, "LISCAP");
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                int idUsu = parseEntero(p[1], "idUsuario");
                boolean ok = capituloDAO.asignarRedactor(idCap, idUsu);
                if (ok) {
                    return generarTarjetaRespuestaHTML("Redactor Asignado al Capítulo", true,
                            "El usuario ha sido designado oficialmente como redactor responsable del capítulo.",
                            new String[][] {
                                    { "ID Capítulo", String.valueOf(idCap) },
                                    { "ID Docente / Redactor", String.valueOf(idUsu) }
                            }, "LISCAP");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Asignar Redactor", false,
                            "No se pudo registrar la asignación. Verifique que tanto el capítulo como el usuario existan en el sistema.",
                            null, "LISCAP");
                }
            }

            if (commandUpper.startsWith("DELASIGCAP")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en DELASIGCAP", false,
                            "Formato esperado: DELASIGCAP[\"idCapitulo\", \"idUsuario\"]",
                            new String[][] { { "Ejemplo Válido", "DELASIGCAP[\"1\", \"3\"]" } }, "LISCAP");
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                int idUsu = parseEntero(p[1], "idUsuario");
                boolean ok = capituloDAO.removerRedactor(idCap, idUsu);
                if (ok) {
                    return generarTarjetaRespuestaHTML("Asignación de Redactor Removida", true,
                            "La designación del docente redactor ID " + idUsu + " para el capítulo ID " + idCap + " ha sido removida.",
                            new String[][] {
                                    { "ID Capítulo", String.valueOf(idCap) },
                                    { "ID Usuario Removido", String.valueOf(idUsu) }
                            }, "LISCAP");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Remover Asignación", false,
                            "No se encontró la asignación especificada para remover.",
                            null, "LISCAP");
                }
            }

            // =========================================================================
            // CU5: MÁQUINA DE ESTADOS, OBSERVACIONES Y AUDITORÍA (RF-06, RF-07, RF-08,
            // RF-10)
            // =========================================================================
            if (commandUpper.startsWith("CAMBESTADO")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 4) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en CAMBESTADO", false,
                            "Formato esperado: CAMBESTADO[\"idCapitulo\", \"nuevoEstado\", \"justificacionTecnica\", \"idUsuario\"]",
                            new String[][] {
                                    { "Estados Permitidos", "'Borrador', 'En Revision', 'Observado', 'Aprobado'" },
                                    { "Ejemplo Válido", "CAMBESTADO[\"1\", \"En Revision\", \"Se completo la primera version\", \"3\"]" }
                            }, "HELP[\"*\"]");
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                String nuevoEstado = p[1].trim();
                String justificacion = p[2].trim();
                int idUsu = parseEntero(p[3], "idUsuario");

                Capitulo cap = capituloDAO.obtenerPorId(idCap);
                if (cap == null) {
                    return generarTarjetaRespuestaHTML("Capítulo No Encontrado", false,
                            "No existe un capítulo curricular con el ID especificado.",
                            new String[][] { { "ID Buscado", String.valueOf(idCap) } }, "LISCAP");
                }

                int nuevoEstadoId = resolverIdEstado(nuevoEstado);
                if (nuevoEstadoId == -1) {
                    return generarTarjetaRespuestaHTML("Estado Inválido", false,
                            "El estado '" + nuevoEstado + "' no es reconocido por la máquina de estados.",
                            new String[][] { { "Estados Válidos", "'Borrador', 'En Revision', 'Observado', 'Aprobado'" } }, "HELP[\"*\"]");
                }

                // VALIDACIÓN ESTRICTA PARA APROBACIÓN (RF-07)
                if (nuevoEstadoId == 4) { // 4 = Aprobado
                    int obsPendientes = observacionDAO.contarObservacionesPendientes(idCap);
                    if (obsPendientes > 0) {
                        return generarTarjetaRespuestaHTML("Rechazo de Aprobación Formal (RF-07)", false,
                                "El Capítulo ID " + idCap + " tiene " + obsPendientes
                                        + " observación(es) técnica(s) pendiente(s) de subsanación.",
                                new String[][] {
                                        { "Capítulo", cap.getTitulo() },
                                        { "Observaciones Pendientes", String.valueOf(obsPendientes) },
                                        { "Regla Académica", "Todas las observaciones deben ser resueltas y aprobadas antes del cierre final" }
                                }, "LISOBS[\"" + idCap + "\"]");
                    }

                    int evidencias = capituloDAO.contarEvidencias(idCap);
                    if (evidencias == 0) {
                        return generarTarjetaRespuestaHTML("Rechazo de Aprobación Formal (RF-07)", false,
                                "El Capítulo ID " + idCap + " no cuenta con ninguna evidencia documental obligatoria vinculada.",
                                new String[][] {
                                        { "Capítulo", cap.getTitulo() },
                                        { "Evidencias Vinculadas", "0 documentos de respaldo" },
                                        { "Regla Normativa", "Debe vincular al menos una resolución o respaldo documental antes de aprobar (RF-13)" }
                                }, "BUSDOC[\"TIPO\", \"Resolucion\"]");
                    }
                }

                String estadoAnterior = cap.getNombreEstado() != null ? cap.getNombreEstado() : "Borrador";
                boolean ok = capituloDAO.cambiarEstado(idCap, nuevoEstadoId);
                if (ok) {
                    historialDAO.registrar(new HistorialModificacion(
                            estadoAnterior,
                            nuevoEstado,
                            "Transición formal de estado",
                            justificacion,
                            idCap,
                            idUsu));
                    return generarTarjetaRespuestaHTML("Transición de Estado Curricular Exitosa", true,
                            "El estado del capítulo ha sido actualizado en el flujo de diseño curricular.",
                            new String[][] {
                                    { "ID Capítulo", String.valueOf(idCap) },
                                    { "Título del Capítulo", cap.getTitulo() },
                                    { "Estado Anterior", estadoAnterior },
                                    { "Nuevo Estado Asignado", nuevoEstado },
                                    { "Justificación Técnica", justificacion },
                                    { "Usuario Auditor ID", String.valueOf(idUsu) }
                            }, "LISHIST[\"" + idCap + "\"]");
                } else {
                    return generarTarjetaRespuestaHTML("Error en Base de Datos", false,
                            "No se pudo actualizar el estado del capítulo en la base de datos.",
                            null, "LISCAP");
                }
            }

            if (commandUpper.startsWith("INSOBS")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 3) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en INSOBS", false,
                            "Formato esperado: INSOBS[\"idCapitulo\", \"comentarioObservacion\", \"idUsuarioAutoridad\"]",
                            new String[][] { { "Ejemplo Válido", "INSOBS[\"1\", \"Falta especificar la bibliografia obligatoria\", \"2\"]" } }, "LISCAP");
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                String comentario = p[1].trim();
                int idAutoridad = parseEntero(p[2], "idUsuarioAutoridad");

                Capitulo cap = capituloDAO.obtenerPorId(idCap);
                if (cap == null) {
                    return generarTarjetaRespuestaHTML("Capítulo No Encontrado", false,
                            "No existe un capítulo curricular con el ID especificado.",
                            new String[][] { { "ID Buscado", String.valueOf(idCap) } }, "LISCAP");
                }

                Observacion obs = new Observacion(comentario, idCap, idAutoridad);
                int idGen = observacionDAO.insertar(obs);
                if (idGen > 0) {
                    capituloDAO.cambiarEstado(idCap, 3); // 3 = Observado
                    historialDAO.registrar(new HistorialModificacion(
                            cap.getNombreEstado(),
                            "Observado",
                            "Se registró la observación técnica #" + idGen,
                            comentario,
                            idCap,
                            idAutoridad));
                    return generarTarjetaRespuestaHTML("Observación Técnica Registrada", true,
                            "La observación técnica ha sido registrada exitosamente. El capítulo pasó automáticamente al estado 'Observado'.",
                            new String[][] {
                                    { "ID Observación", String.valueOf(idGen) },
                                    { "ID Capítulo", String.valueOf(idCap) },
                                    { "Título del Capítulo", cap.getTitulo() },
                                    { "Comentario de Observación", comentario },
                                    { "Nuevo Estado del Capítulo", "Observado" },
                                    { "Autoridad Evaluadora ID", String.valueOf(idAutoridad) }
                            }, "LISOBS[\"" + idCap + "\"]");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Registrar Observación", false,
                            "No se pudo guardar la observación técnica en la base de datos.",
                            null, "LISCAP");
                }
            }

            if (commandUpper.startsWith("SUBSANOBS")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 3) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en SUBSANOBS", false,
                            "Formato esperado: SUBSANOBS[\"idObservacion\", \"respuestaSubsanacion\", \"idUsuarioDelegado\"]",
                            new String[][] { { "Ejemplo Válido", "SUBSANOBS[\"1\", \"Se incorporaron las citas en formato APA septima edicion\", \"3\"]" } }, "HELP[\"*\"]");
                }
                int idObs = parseEntero(p[0], "idObservacion");
                String respuesta = p[1].trim();
                int idDelegado = parseEntero(p[2], "idUsuarioDelegado");

                Observacion obs = observacionDAO.obtenerPorId(idObs);
                if (obs == null) {
                    return generarTarjetaRespuestaHTML("Observación No Encontrada", false,
                            "No existe una observación técnica con el ID especificado.",
                            new String[][] { { "ID Buscado", String.valueOf(idObs) } }, "HELP[\"*\"]");
                }

                boolean ok = observacionDAO.subsanar(idObs, respuesta, idDelegado);
                if (ok) {
                    return generarTarjetaRespuestaHTML("Observación Técnica Subsanada", true,
                            "La subsanación ha sido registrada formalmente en el sistema.",
                            new String[][] {
                                    { "ID Observación", String.valueOf(idObs) },
                                    { "Capítulo Asociado ID", String.valueOf(obs.getIdCapitulo()) },
                                    { "Usuario Redactor ID", String.valueOf(idDelegado) },
                                    { "Respuesta Técnica", respuesta },
                                    { "Estado Observación", "Subsanada" }
                            }, "LISOBS[\"" + obs.getIdCapitulo() + "\"]");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Subsanar Observación", false,
                            "No se pudo registrar la respuesta de subsanación.",
                            null, "LISOBS[\"" + obs.getIdCapitulo() + "\"]");
                }
            }

            if (commandUpper.startsWith("LISOBS")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 1) {
                    return "Error de sintaxis en LISOBS: Formato esperado: LISOBS[\"idCapitulo\"]\nEjemplo: LISOBS[\"1\"]";
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                List<Observacion> lista = observacionDAO.listarPorCapitulo(idCap);
                if (lista.isEmpty()) {
                    return "El Capítulo ID " + idCap + " no tiene observaciones técnicas registradas.";
                }
                return generarTablaObservacionesHTML(idCap, lista);
            }

            if (commandUpper.startsWith("LISHIST")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 1) {
                    return "Error de sintaxis en LISHIST: Formato esperado: LISHIST[\"idCapitulo\"]\nEjemplo: LISHIST[\"1\"]";
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                List<HistorialModificacion> lista = historialDAO.listarPorCapitulo(idCap);
                if (lista.isEmpty()) {
                    return "El Capítulo ID " + idCap + " no registra modificaciones en la pista de auditoría.";
                }
                return generarTablaHistorialHTML(idCap, lista);
            }

            // =========================================================================
            // CU6: REPOSITORIO DE EVIDENCIAS Y BÚSQUEDA FACETADA (RF-11, RF-12, RF-13,
            // RF-14)
            // =========================================================================
            if (commandUpper.startsWith("INSDOC")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 6) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en INSDOC", false,
                            "Se esperaban al menos 6 parámetros obligatorios para registrar un documento de evidencia.",
                            new String[][] {
                                    { "Formato Requerido", "INSDOC[\"codigoNorma\", \"titulo\", \"tipoDoc\", \"organoEmisor\", \"fechaEmision(AAAA-MM-DD)\", \"archivoRuta\", \"idDocPadre(opcional)\"]" },
                                    { "Ejemplo Válido", "INSDOC[\"RES-045/2026\", \"Resolucion de Comision\", \"Resolucion\", \"Consejo Facultativo\", \"2026-03-15\", \"https://drive.google.com/res045.pdf\", \"0\"]" }
                            }, "LISDOC", GUIA_DOCUMENTO_PDF);
                }
                Date fecha = parseFecha(p[4], "fechaEmision");
                Integer idPadre = (p.length >= 7 && !p[6].trim().isEmpty() && !p[6].trim().equals("0"))
                        ? parseEntero(p[6], "idDocPadre")
                        : null;

                DocumentoEvidencia doc = new DocumentoEvidencia(p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(),
                        fecha, p[5].trim(), idPadre);
                int idGen = documentoDAO.insertar(doc);
                if (idGen > 0) {
                    return generarTarjetaRespuestaHTML("Documento de Evidencia Registrado", true,
                            "La evidencia documental ha sido incorporada al repositorio oficial del rediseño.",
                            new String[][] {
                                    { "ID Asignado", String.valueOf(idGen) },
                                    { "Código de Norma", p[0].trim() },
                                    { "Título / Descripción", p[1].trim() },
                                    { "Tipo de Documento", p[2].trim() },
                                    { "Órgano Emisor", p[3].trim() },
                                    { "Fecha de Emisión", p[4].trim() },
                                    { "Archivo / Enlace", p[5].trim() },
                                    { "Norma Jerárquica Padre", idPadre != null ? String.valueOf(idPadre) : "Norma Raíz" }
                            }, "LISDOC");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Registrar Documento", false,
                            "Error al registrar evidencia. Verifique que el código de norma sea único en el repositorio.",
                            new String[][] { { "Código Intentado", p[0].trim() } }, "LISDOC");
                }
            }

            if (commandUpper.startsWith("LISDOC")) {
                String[] p = extraerParams(trimmed);
                List<DocumentoEvidencia> docs;
                if (p != null && p.length >= 1 && !p[0].equals("*") && !p[0].isEmpty()) {
                    int idCap = parseEntero(p[0], "idCapitulo");
                    docs = documentoDAO.listarEvidenciasDeCapitulo(idCap);
                } else {
                    docs = documentoDAO.listarTodos();
                }

                if (docs.isEmpty()) {
                    return "No existen documentos de evidencia registrados para el criterio solicitado.";
                }
                return generarTablaDocumentosHTML(docs, "Catálogo de Evidencias y Resoluciones");
            }

            if (commandUpper.startsWith("VINCULEVI")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en VINCULEVI", false,
                            "Formato esperado: VINCULEVI[\"idCapitulo\", \"idDocumentoEvidencia\"]",
                            new String[][] { { "Ejemplo Válido", "VINCULEVI[\"1\", \"5\"]" } }, "LISDOC");
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                int idDoc = parseEntero(p[1], "idDocumentoEvidencia");

                boolean ok = documentoDAO.vincularACapitulo(idCap, idDoc);
                if (ok) {
                    return generarTarjetaRespuestaHTML("Evidencia Vinculada Exitosamente", true,
                            "El documento de evidencia ha sido vinculado como respaldo formal al capítulo curricular.",
                            new String[][] {
                                    { "ID Capítulo", String.valueOf(idCap) },
                                    { "ID Evidencia Documental", String.valueOf(idDoc) },
                                    { "Cumplimiento Normativo", "Acreditado para aprobación formal (RF-13)" }
                            }, "LISDOC[\"" + idCap + "\"]");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Vincular Evidencia", false,
                            "No se pudo vincular la evidencia. Verifique que tanto el capítulo como el documento existan.",
                            null, "LISDOC");
                }
            }

            if (commandUpper.startsWith("DELVINCULEVI")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en DELVINCULEVI", false,
                            "Formato esperado: DELVINCULEVI[\"idCapitulo\", \"idDocumentoEvidencia\"]",
                            new String[][] { { "Ejemplo Válido", "DELVINCULEVI[\"1\", \"5\"]" } }, "LISDOC");
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                int idDoc = parseEntero(p[1], "idDocumentoEvidencia");

                boolean ok = documentoDAO.desvincularDeCapitulo(idCap, idDoc);
                if (ok) {
                    return generarTarjetaRespuestaHTML("Evidencia Desvinculada Exitosamente", true,
                            "El documento ID " + idDoc + " ha sido desvinculado del capítulo ID " + idCap + ".",
                            new String[][] {
                                    { "ID Capítulo", String.valueOf(idCap) },
                                    { "ID Evidencia Desvinculada", String.valueOf(idDoc) }
                            }, "LISDOC[\"" + idCap + "\"]");
                } else {
                    return generarTarjetaRespuestaHTML("Error al Desvincular Evidencia", false,
                            "No se encontró la vinculación especificada para desvincular.",
                            null, "LISDOC");
                }
            }

            if (commandUpper.startsWith("BUSDOC")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en BUSDOC", false,
                            "Formato esperado: BUSDOC[\"criterio\", \"valor\"]",
                            new String[][] {
                                    { "Criterios Permitidos", "'TIPO', 'EMISOR', 'CODIGO', 'CAPITULO', 'VIGENTE' o texto libre" },
                                    { "Ejemplo Válido", "BUSDOC[\"TIPO\", \"Resolucion\"]" }
                            }, "LISDOC");
                }
                String criterio = p[0].trim();
                String valor = p[1].trim();

                List<DocumentoEvidencia> docs = documentoDAO.busquedaFacetada(criterio, valor);
                if (docs.isEmpty()) {
                    return generarTarjetaRespuestaHTML("Búsqueda sin Resultados", false,
                            "No se encontraron documentos de evidencia para el criterio '" + criterio + " = " + valor + "'.",
                            new String[][] { { "Criterio", criterio }, { "Valor Buscado", valor } }, "LISDOC");
                }
                return generarTablaDocumentosHTML(docs,
                        "Resultado de Búsqueda Facetada (" + criterio + ": " + valor + ")");
            }

            // =========================================================================
            // CU7: TABLERO DE CONTROL (DASHBOARD) Y COMPILADOR FINAL (RF-09, RF-15)
            // =========================================================================
            if (commandUpper.startsWith("VERDASH")) {
                ReporteDAO.MetricasDashboard metricas = reporteDAO.obtenerMetricasDashboard();
                return generarDashboardHTML(metricas);
            }

            if (commandUpper.startsWith("REPORTEFINAL")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 1) {
                    return generarTarjetaRespuestaHTML("Error de Sintaxis en REPORTEFINAL", false,
                            "Formato esperado: REPORTEFINAL[\"idPlanEstudio\"]",
                            new String[][] { { "Ejemplo Válido", "REPORTEFINAL[\"1\"]" } }, "LISPLA");
                }
                int idPlan = parseEntero(p[0], "idPlanEstudio");
                ReporteDAO.ReporteFinalConsolidado rep = reporteDAO.compilarReporteFinal(idPlan);
                if (rep == null || rep.plan == null) {
                    return generarTarjetaRespuestaHTML("Plan No Encontrado", false,
                            "No existe un Plan de Estudios con el ID " + idPlan + " para compilar.",
                            new String[][] { { "ID Plan Buscado", String.valueOf(idPlan) } }, "LISPLA");
                }
                return generarReporteFinalHTML(rep);
            }

            // =========================================================================
            // CU8: REPORTES Y ESTADÍSTICAS GENERALES
            // =========================================================================
            if (commandUpper.startsWith("REPAVANCE")) {
                List<ReporteDAO.EstadisticaComponente> comps = reporteDAO.obtenerEstadisticasPorComponente();
                return generarReporteAvanceHTML(comps);
            }

            if (commandUpper.startsWith("REPUSUARIOS")) {
                List<ReporteDAO.EstadisticaUsuario> usuarios = reporteDAO.obtenerEstadisticasUsuarios();
                return generarReporteUsuariosHTML(usuarios);
            }

            return generarTarjetaRespuestaHTML("Comando No Reconocido", false,
                    "El comando recibido en el asunto '" + subject + "' no fue reconocido por el sistema.",
                    new String[][] {
                            { "Asunto Recibido", subject },
                            { "Sugerencia", "Envíe HELP[\"*\"] para consultar el manual del sistema con todos los comandos y ejemplos." }
                    }, "HELP[\"*\"]");

        } catch (NumberFormatException e) {
            return generarTarjetaRespuestaHTML("Error en Parámetro Numérico", false,
                    e.getMessage(), null, "HELP[\"*\"]");
        } catch (IllegalArgumentException e) {
            return generarTarjetaRespuestaHTML("Error en Parámetro de Fecha u Opción", false,
                    e.getMessage(), null, "HELP[\"*\"]");
        } catch (Exception e) {
            return generarTarjetaRespuestaHTML("Error al Procesar Comando", false,
                    e.getMessage() != null ? e.getMessage() : "Error interno del servidor", null, "HELP[\"*\"]");
        }
    }

    private int parseEntero(String valor, String nombreCampo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new NumberFormatException("El campo '" + nombreCampo + "' es obligatorio y no puede estar vacío.");
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("El campo '" + nombreCampo
                    + "' debe ser un número entero válido (valor recibido: '" + valor.trim() + "').");
        }
    }

    private Date parseFecha(String valor, String nombreCampo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo '" + nombreCampo
                    + "' es obligatorio y no puede estar vacío. Formato requerido: AAAA-MM-DD.");
        }
        try {
            return Date.valueOf(valor.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El campo '" + nombreCampo
                    + "' debe tener el formato de fecha AAAA-MM-DD (ejemplo: 2026-10-15). Valor recibido: '"
                    + valor.trim() + "'.");
        }
    }

    private String verificarPermisos(String comando, String remitente) {
        // Comandos públicos que no requieren sesión/rol previo:
        if (comando.startsWith("HELP") || comando.startsWith("LOGIN") || comando.startsWith("LISPLA")
                || comando.startsWith("LISASIG") || comando.startsWith("VERGRAFO")) {
            return null; // Permitido públicamente
        }

        if (remitente == null || remitente.trim().isEmpty()) {
            return "Acceso denegado: No se pudo identificar la dirección de correo remitente.";
        }

        Usuario remitenteUser = usuarioDAO.obtenerPorEmail(remitente);
        if (remitenteUser == null) {
            return generarAlertaAccesoDenegadoHTML(remitente, comando);
        }

        int rol = remitenteUser.getIdRol();
        // Rol 1 = Director (acceso total a todas las operaciones)
        if (rol == 1) {
            return null;
        }

        // Restricciones para Rol 2 (Autoridad / Delegado Curricular)
        if (rol == 2) {
            if (comando.startsWith("INSUSU") || comando.startsWith("UPDUSU") || comando.startsWith("DELUSU")) {
                return generarTarjetaRespuestaHTML("Permiso Denegado", false,
                        "Su rol 'Delegado Curricular' no tiene permisos para gestionar usuarios (INSUSU/UPDUSU/DELUSU). Acción reservada para Administradores - Director.",
                        new String[][] { { "Rol Remitente", "Delegado Curricular (Rol 2)" }, { "Comando Solicitado", comando } }, "HELP[\"*\"]");
            }
            if (comando.startsWith("INSPLA") || comando.startsWith("UPDPLA")) {
                return generarTarjetaRespuestaHTML("Permiso Denegado", false,
                        "Su rol 'Delegado Curricular' no tiene permisos para crear o modificar planes de estudio. Acción reservada para Administradores - Director.",
                        new String[][] { { "Rol Remitente", "Delegado Curricular (Rol 2)" }, { "Comando Solicitado", comando } }, "HELP[\"*\"]");
            }
            return null; // Habilitado para componentes, capítulos, observaciones, evidencias, tableros y reporte final
        }

        // Restricciones para Rol 3 (Invitado Redactor)
        if (rol == 3) {
            if (comando.startsWith("INSUSU") || comando.startsWith("UPDUSU") || comando.startsWith("DELUSU") ||
                    comando.startsWith("INSPLA") || comando.startsWith("UPDPLA") ||
                    comando.startsWith("INSASIG") || comando.startsWith("UPDASIG") || comando.startsWith("DELASIG") ||
                    comando.startsWith("INSPRE") || comando.startsWith("DELPRE") ||
                    comando.startsWith("INSCOM") || comando.startsWith("ASIGCAP") || comando.startsWith("DELASIGCAP") ||
                    comando.startsWith("CAMBESTADO") || comando.startsWith("INSOBS")) {
                return generarTarjetaRespuestaHTML("Permiso Denegado", false,
                        "Su rol 'Invitado Redactor' no tiene autorización para ejecutar el comando '" + comando + "'. Sus permisos están limitados a redactar capítulos (INSCAP/UPDCAP), subsanar observaciones (SUBSANOBS), vincular evidencias (VINCULEVI) y consultar su avance.",
                        new String[][] { { "Rol Remitente", "Invitado Redactor (Rol 3)" }, { "Comando Solicitado", comando } }, "HELP[\"*\"]");
            }
            return null;
        }

        return null;
    }

    private String generarAlertaAccesoDenegadoHTML(String remitente, String comando) {
        return "<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px; line-height: 1.5;'>" +
                "<div style='background-color: #c0392b; color: white; padding: 15px 20px; border-radius: 6px; max-width: 650px;'>"
                +
                "<h2 style='margin: 0; font-size: 20px;'>🚫 Acceso Restringido - Autenticación Requerida</h2>" +
                "</div>" +
                "<div style='background-color: #fdf7f7; border: 1px solid #ebccd1; padding: 18px; border-radius: 6px; margin-top: 15px; max-width: 650px;'>"
                +
                "<p style='margin: 0 0 10px 0; font-size: 14px; color: #a94442;'>" +
                "La dirección de correo remitente <b>&lt;" + remitente
                + "&gt;</b> no se encuentra vinculada a ningún usuario activo en el sistema." +
                "</p>" +
                "<p style='margin: 0; font-size: 13px; color: #555;'>" +
                "<b>Comando solicitado:</b> <code style='background: #eee; padding: 2px 6px; border-radius: 3px; color: #c0392b;'>"
                + comando + "</code>" +
                "</p>" +
                "</div>" +
                "<div style='background-color: #f7f9fc; border: 1px solid #e1e8ed; padding: 18px; border-radius: 6px; margin-top: 15px; max-width: 650px;'>"
                +
                "<h4 style='margin: 0 0 10px 0; color: #003366;'>¿Cómo vincular su correo y autorizar sus operaciones?</h4>"
                +
                "<p style='font-size: 13px; margin: 0 0 10px 0; color: #444;'>" +
                "Para iniciar sesión y asociar este buzón de correo a su cuenta institucional, envíe en el <b>Asunto</b> de un nuevo correo:"
                +
                "</p>" +
                "<div style='background-color: #eef2f7; border-left: 4px solid #003366; padding: 12px; font-family: monospace; font-size: 13px; color: #003366; margin-bottom: 10px;'>"
                +
                "LOGIN[\"su_correo@institucional.edu.bo\", \"su_contraseña\"]" +
                "</div>" +
                "<p style='font-size: 12px; color: #666; margin: 0;'>" +
                "• <i>Ejemplo Director Inicial:</i> <code>LOGIN[\"grupo02sa@tecnoweb.org.bo\", \"admin123\"]</code>"
                +
                "</p>" +
                "<hr style='border: 0; border-top: 1px solid #e1e8ed; margin: 15px 0;'/>" +
                "<h4 style='margin: 0 0 8px 0; color: #003366;'>Comandos de libre consulta (públicos sin login):</h4>" +
                "<p style='font-size: 12px; color: #555; margin: 0; line-height: 1.6;'>" +
                "• <code>HELP[\"*\"]</code> : Manual universal de comandos y sintaxis.<br/>" +
                "• <code>LISPLA[\"*\"]</code> : Catálogo de planes de estudio vigentes.<br/>" +
                "• <code>LISASIG[\"*\"]</code> : Malla curricular y asignaturas registradas.<br/>" +
                "• <code>VERGRAFO[\"idPlan\"]</code> : Grafo analítico de prerrequisitos por niveles." +
                "</p>" +
                "</div>" +
                "<p style='font-size: 11px; color: #888; margin-top: 15px;'>Sistema de Seguimiento al Diseño Curricular - UAGRM TecnoWeb 2026</p>"
                +
                "</body></html>";
    }

    private static final String GUIA_FOTO_USUARIO =
            "<h4 style='margin: 0 0 6px 0; color: #16a085; font-size: 13px;'>💡 Guía para Foto de Usuario:</h4>" +
            "<p style='margin: 0; font-size: 12px; color: #555; line-height: 1.4;'>" +
            "• <b>Enlace en la Nube (Recomendado):</b> Puede ingresar una URL pública a una imagen web (ej: <code>https://i.imgur.com/avatar.png</code> o <code>https://i.pravatar.cc/150?u=croca</code>).<br/>" +
            "• <b>Ruta Local:</b> Puede indicar una ruta en el servidor como <code>fotos/croca.png</code>.<br/>" +
            "• <b>Avatar Automático:</b> Si deja <code>\"fotos/default.png\"</code> o un texto vacío, el sistema generará automáticamente un avatar circular con las iniciales del usuario y color de su rol." +
            "</p>";

    private static final String GUIA_DOCUMENTO_PDF =
            "<h4 style='margin: 0 0 6px 0; color: #16a085; font-size: 13px;'>💡 Guía para Documentos PDF y Evidencias:</h4>" +
            "<p style='margin: 0; font-size: 12px; color: #555; line-height: 1.4;'>" +
            "• <b>Enlace en la Nube con Google Drive (Recomendado):</b><br/>" +
            "  1. Suba su archivo PDF a su Google Drive o repositorio público.<br/>" +
            "  2. En Drive, haga clic en <i>Compartir</i> y seleccione: <b>'Cualquier persona con el enlace (Lector)'</b>.<br/>" +
            "  3. Pegue el enlace obtenido en el parámetro <code>archivoRuta</code> (ej: <code>https://drive.google.com/file/d/1A2B3C.../view</code>).<br/>" +
            "• <b>Ruta Local en el Servidor:</b> Puede ingresar <code>storage/documentos/mi_archivo.pdf</code> si el archivo reside en el servidor.<br/>" +
            "• <b>Visualización:</b> Al consultar con <code>LISDOC</code> o <code>BUSDOC</code>, se generará un botón interactivo para ver y descargar el documento directamente." +
            "</p>";

    private String generarTarjetaRespuestaHTML(String titulo, boolean exito, String mensajePrincipal,
            String[][] detalles, String comandoSiguiente) {
        return generarTarjetaRespuestaHTML(titulo, exito, mensajePrincipal, detalles, comandoSiguiente, null);
    }

    private String generarTarjetaRespuestaHTML(String titulo, boolean exito, String mensajePrincipal,
            String[][] detalles, String comandoSiguiente, String guiaExtraHTML) {
        String colorPrincipal = exito ? "#27ae60" : "#c0392b";
        String fondoHeader = exito ? "#eafaf1" : "#fdedec";
        String icono = exito ? "✔ " : "✖ ";

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #2c3e50; margin: 20px; line-height: 1.5;'>");
        sb.append("<div style='max-width: 650px; border: 1px solid #dcdde1; border-top: 5px solid ")
                .append(colorPrincipal).append("; border-radius: 6px; background-color: #ffffff; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.05);'>");

        sb.append("<div style='background-color: ").append(fondoHeader).append("; padding: 12px 15px; border-radius: 4px; margin-bottom: 15px;'>")
                .append("<h3 style='margin: 0; color: ").append(colorPrincipal).append("; font-size: 18px;'>")
                .append(icono).append(titulo).append("</h3>")
                .append("</div>");

        sb.append("<p style='font-size: 14px; margin-bottom: 15px; color: #34495e;'>")
                .append(mensajePrincipal)
                .append("</p>");

        if (detalles != null && detalles.length > 0) {
            sb.append("<table cellpadding='6' cellspacing='0' style='width: 100%; border-collapse: collapse; font-size: 13px; margin-bottom: 15px; background-color: #fafbfc; border: 1px solid #ebedf0; border-radius: 4px;'>");
            for (String[] fila : detalles) {
                if (fila != null && fila.length >= 2) {
                    sb.append("<tr style='border-bottom: 1px solid #ebedf0;'>")
                            .append("<td style='padding: 8px 12px; width: 35%; color: #7f8c8d; font-weight: bold;'>").append(fila[0]).append(":</td>")
                            .append("<td style='padding: 8px 12px; color: #2c3e50;'>").append(fila[1]).append("</td>")
                            .append("</tr>");
                }
            }
            sb.append("</table>");
        }

        if (guiaExtraHTML != null && !guiaExtraHTML.trim().isEmpty()) {
            sb.append("<div style='background-color: #fdfefe; border: 1px solid #d4efdf; border-left: 4px solid #1abc9c; padding: 12px 15px; border-radius: 4px; margin-bottom: 15px;'>")
                    .append(guiaExtraHTML)
                    .append("</div>");
        }

        if (comandoSiguiente != null && !comandoSiguiente.trim().isEmpty()) {
            sb.append("<div style='background-color: #f8f9fa; border-left: 3px solid #2980b9; padding: 10px 14px; border-radius: 0 4px 4px 0; margin-top: 15px;'>")
                    .append("<span style='font-size: 12px; color: #7f8c8d; text-transform: uppercase; font-weight: bold;'>Próximo Comando Recomendado:</span><br/>")
                    .append("<code style='font-size: 13px; color: #2980b9; font-weight: bold; background: #e8f4fc; padding: 2px 6px; border-radius: 3px; display: inline-block; margin-top: 4px;'>")
                    .append(comandoSiguiente)
                    .append("</code>")
                    .append("</div>");
        }

        sb.append("<div style='margin-top: 20px; border-top: 1px solid #eee; padding-top: 10px; font-size: 11px; color: #95a5a6; text-align: center;'>")
                .append("Sistema de Seguimiento al Diseño Curricular — UAGRM TecnoWeb 2026")
                .append("</div>");

        sb.append("</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String generarNotificacionCredencialesHTML(String nombre, String email, String username, String passPlana, int idRol) {
        String nombreRol = idRol == 1 ? "Director de Carrera" : (idRol == 2 ? "Delegado Curricular / Autoridad" : "Docente Redactor / Invitado");
        String rolColor = idRol == 1 ? "#c0392b" : (idRol == 2 ? "#2980b9" : "#27ae60");

        return "<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px; line-height: 1.5;'>" +
                "<div style='max-width: 650px; border: 1px solid #dcdde1; border-top: 5px solid #003366; border-radius: 6px; background-color: #ffffff; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.05);'>" +
                "<div style='background-color: #003366; color: white; padding: 15px; border-radius: 4px; margin-bottom: 15px;'>" +
                "<h2 style='margin: 0; font-size: 20px;'>Bienvenido al Sistema de Seguimiento Curricular</h2>" +
                "<p style='margin: 4px 0 0 0; font-size: 13px; opacity: 0.9;'>Universidad Autónoma Gabriel René Moreno - Grupo 02 SA</p>" +
                "</div>" +
                "<p>Estimado(a) <b>" + nombre + "</b>,</p>" +
                "<p>Se le ha otorgado acceso al <b>Sistema de Seguimiento al Rediseño Curricular por Correo Electrónico</b>. A continuación encontrará sus credenciales oficiales de acceso:</p>" +
                "<div style='background-color: #f7f9fc; border: 1px solid #e1e8ed; padding: 15px; border-radius: 4px; margin: 15px 0;'>" +
                "<table cellpadding='6' cellspacing='0' style='width: 100%; border-collapse: collapse; font-size: 13px;'>" +
                "<tr><td style='width: 35%; color: #666; font-weight: bold;'>Nombre Completo:</td><td><b>" + nombre + "</b></td></tr>" +
                "<tr><td style='color: #666; font-weight: bold;'>Correo Electrónico:</td><td>" + email + "</td></tr>" +
                "<tr><td style='color: #666; font-weight: bold;'>Nombre de Usuario:</td><td><code style='font-size: 14px; color: #003366; font-weight: bold;'>" + username + "</code></td></tr>" +
                "<tr><td style='color: #666; font-weight: bold;'>Contraseña Temporal:</td><td><code style='font-size: 14px; color: #c0392b; font-weight: bold; background: #fbeee6; padding: 2px 6px; border-radius: 3px;'>" + passPlana + "</code></td></tr>" +
                "<tr><td style='color: #666; font-weight: bold;'>Rol Asignado:</td><td><span style='background-color: " + rolColor + "; color: white; padding: 3px 8px; border-radius: 3px; font-weight: bold; font-size: 12px;'>" + nombreRol + "</span></td></tr>" +
                "</table>" +
                "</div>" +
                "<h4 style='color: #003366; margin-top: 15px; margin-bottom: 8px;'>¿Cómo iniciar sesión por correo?</h4>" +
                "<p style='font-size: 13px; color: #444; margin-bottom: 6px;'>Para vincular su buzón y recibir el menú de operaciones correspondiente a su rol, envíe un correo electrónico a <b>" + Config.MAIL_EMAIL + "</b> con el siguiente Asunto:</p>" +
                "<div style='background-color: #edf2f7; border-left: 3px solid #003366; padding: 10px 14px; border-radius: 0 4px 4px 0; font-family: monospace; font-size: 13px; font-weight: bold; color: #2d3748;'>" +
                "LOGIN[\"" + email + "\", \"" + passPlana + "\"]" +
                "</div>" +
                "<div style='margin-top: 20px; border-top: 1px solid #eee; padding-top: 10px; font-size: 11px; color: #95a5a6; text-align: center;'>" +
                "Facultad de Contaduría Pública y Sistemas — UAGRM TecnoWeb 2026" +
                "</div>" +
                "</div></body></html>";
    }

    private String obtenerIniciales(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) {
            return "U";
        }
        String[] partes = nombreCompleto.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
        return ("" + partes[0].charAt(0) + partes[partes.length - 1].charAt(0)).toUpperCase();
    }

    private String generarTablaComandosRolHTML(int idRol) {
        StringBuilder sb = new StringBuilder();
        String encabezadoColor = idRol == 1 ? "#003366" : (idRol == 2 ? "#2980b9" : "#27ae60");
        sb.append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 12px; border-color: #ddd; margin-top: 12px;'>")
                .append("<tr style='background-color: ").append(encabezadoColor).append("; color: white;'>")
                .append("<th>Caso de Uso</th><th>Comando</th><th>Parámetros Requeridos</th><th>Descripción y Ejemplo Listo para Enviar</th>")
                .append("</tr>");

        if (idRol == 1) { // Director / Administrador
            sb.append("<tr><td rowspan='4'><b>Gestión de Usuarios</b></td>")
                    .append("<td><code>LISUSU</code></td><td><i>Ninguno</i></td><td>Listar usuarios registrados:<br/><code>LISUSU</code></td></tr>")
                    .append("<tr><td><code>INSUSU</code></td><td>nombre, email, user, pass, rol, foto</td><td>Registrar usuario y notificar llaves:<br/><code>INSUSU[\"Lic. Carlos Roca\", \"croca@uagrm.edu.bo\", \"croca\", \"Admin2026ñ\", \"3\", \"fotos/croca.png\"]</code></td></tr>")
                    .append("<tr><td><code>UPDUSU</code></td><td>id, nom, email, user, pass, rol, foto, act</td><td>Actualizar usuario:<br/><code>UPDUSU[\"1\", \"Carlos Roca\", \"croca@uagrm.edu.bo\", \"croca\", \"Admin2026ñ\", \"3\", \"fotos/croca.png\", \"true\"]</code></td></tr>")
                    .append("<tr><td><code>DELUSU</code></td><td>idUsuario</td><td>Eliminar usuario:<br/><code>DELUSU[\"2\"]</code></td></tr>");

            sb.append("<tr><td rowspan='3'><b>Planes y Materias</b></td>")
                    .append("<td><code>LISPLA</code> / <code>INSPLA</code></td><td>cod, carrera, anio, desc, obj, perf, min, max</td><td>Crear plan de estudios:<br/><code>INSPLA[\"SIS-2026\", \"Ing. Sistemas\", \"2026\", \"Plan 2026\", \"Obj\", \"Perfil\", \"200\", \"240\"]</code></td></tr>")
                    .append("<tr><td><code>LISASIG</code></td><td>idPlan (opcional)</td><td>Listar materias del plan:<br/><code>LISASIG[\"1\"]</code></td></tr>")
                    .append("<tr><td><code>INSASIG</code></td><td>sigla, nom, sem, ht, hp, cred, area, idPlan</td><td>Registrar asignatura:<br/><code>INSASIG[\"INF110\", \"Intro a Informatica\", \"1\", \"4\", \"2\", \"5\", \"Computacion\", \"1\"]</code></td></tr>");

            sb.append("<tr><td rowspan='2'><b>Prerrequisitos y Malla</b></td>")
                    .append("<td><code>INSPRE</code> / <code>DELPRE</code></td><td>idAsig, idAsigRequisito</td><td>Vincular dependencia (valida ciclos RF-03):<br/><code>INSPRE[\"5\", \"1\"]</code></td></tr>")
                    .append("<tr><td><code>VERGRAFO</code></td><td>idAsignatura</td><td>Grafo analítico interactivo:<br/><code>VERGRAFO[\"5\"]</code></td></tr>");

            sb.append("<tr><td rowspan='3'><b>Rediseño Curricular</b></td>")
                    .append("<td><code>INSCOM</code> / <code>LISCOM</code></td><td>num, nom, desc, idPlan</td><td>Registrar componente estructural:<br/><code>INSCOM[\"1\", \"Marco Referencial\", \"Bases institucionales\", \"1\"]</code></td></tr>")
                    .append("<tr><td><code>INSCAP</code> / <code>LISCAP</code></td><td>num, titulo, contenido, idComp</td><td>Crear capítulo en Borrador:<br/><code>INSCAP[\"1\", \"Diagnostico Profesional\", \"Estudio de demanda...\", \"1\"]</code></td></tr>")
                    .append("<tr><td><code>ASIGCAP</code> / <code>DELASIGCAP</code></td><td>idCapitulo, idUsuario</td><td>Designar docente redactor:<br/><code>ASIGCAP[\"1\", \"3\"]</code></td></tr>");

            sb.append("<tr><td rowspan='3'><b>Estados y Control</b></td>")
                    .append("<td><code>CAMBESTADO</code></td><td>idCap, nuevoEstado, justificacion, idUsu</td><td>Transición de estado formal:<br/><code>CAMBESTADO[\"1\", \"En Revision\", \"Se completo redaccion\", \"1\"]</code></td></tr>")
                    .append("<tr><td><code>INSOBS</code> / <code>SUBSANOBS</code></td><td>idCap, comentario, idAutoridad</td><td>Observar capítulo técnico:<br/><code>INSOBS[\"1\", \"Completar bibliografia APA\", \"1\"]</code></td></tr>")
                    .append("<tr><td><code>LISOBS</code> / <code>LISHIST</code></td><td>idCapitulo</td><td>Pista de auditoría histórica:<br/><code>LISHIST[\"1\"]</code></td></tr>");

            sb.append("<tr><td rowspan='2'><b>Evidencias Normativas</b></td>")
                    .append("<td><code>INSDOC</code> / <code>LISDOC</code></td><td>cod, titulo, tipo, emisor, fecha, url, idPadre</td><td>Registrar evidencia o resolución:<br/><code>INSDOC[\"RES-045/2026\", \"Resolucion CF\", \"Resolucion\", \"CF\", \"2026-03-15\", \"https://drive.google.com/doc.pdf\", \"0\"]</code></td></tr>")
                    .append("<tr><td><code>VINCULEVI</code> / <code>BUSDOC</code></td><td>idCap, idDoc / criterio, valor</td><td>Respaldar capítulo (RF-13) / Búsqueda:<br/><code>VINCULEVI[\"1\", \"1\"]</code><br/><code>BUSDOC[\"TIPO\", \"Resolucion\"]</code></td></tr>");

            sb.append("<tr><td><b>Compilador y Métricas</b></td>")
                    .append("<td><code>VERDASH</code><br/><code>REPORTEFINAL</code><br/><code>REPAVANCE</code></td><td>idPlanEstudio</td><td>Tablero ejecutivo y compilador oficial:<br/><code>VERDASH</code><br/><code>REPORTEFINAL[\"1\"]</code></td></tr>");

        } else if (idRol == 2) { // Delegado Curricular / Autoridad
            sb.append("<tr><td><b>Rediseño Curricular</b></td>")
                    .append("<td><code>LISCOM</code> / <code>INSCOM</code><br/><code>LISCAP</code></td><td>num, nom, desc, idPlan</td><td>Supervisar componentes y capítulos:<br/><code>INSCOM[\"1\", \"Marco Referencial\", \"Bases\", \"1\"]</code><br/><code>LISCAP[\"1\"]</code></td></tr>");

            sb.append("<tr><td rowspan='3'><b>Evaluación y Estados</b></td>")
                    .append("<td><code>CAMBESTADO</code></td><td>idCap, nuevoEstado, justificacion, idUsu</td><td>Aprobar o transicionar capítulo:<br/><code>CAMBESTADO[\"1\", \"Aprobado\", \"Cumple requisitos RF-07\", \"2\"]</code></td></tr>")
                    .append("<tr><td><code>INSOBS</code></td><td>idCap, comentario, idAutoridad</td><td>Observar capítulo (pasa a 'Observado'):<br/><code>INSOBS[\"1\", \"Falta marco teorico\", \"2\"]</code></td></tr>")
                    .append("<tr><td><code>LISOBS</code> / <code>LISHIST</code></td><td>idCapitulo</td><td>Consultar observaciones y auditoría:<br/><code>LISOBS[\"1\"]</code></td></tr>");

            sb.append("<tr><td rowspan='2'><b>Evidencias Normativas</b></td>")
                    .append("<td><code>INSDOC</code> / <code>LISDOC</code></td><td>cod, titulo, tipo, emisor, fecha, url, idPadre</td><td>Ingresar resoluciones y respaldos:<br/><code>INSDOC[\"RES-012/2026\", \"Aprobacion\", \"Resolucion\", \"CF\", \"2026-02-10\", \"https://...\", \"0\"]</code></td></tr>")
                    .append("<tr><td><code>BUSDOC</code> / <code>VINCULEVI</code></td><td>criterio, valor / idCap, idDoc</td><td>Búsqueda facetada y vinculación:<br/><code>BUSDOC[\"TIPO\", \"Resolucion\"]</code></td></tr>");

            sb.append("<tr><td><b>Compilador Oficial</b></td>")
                    .append("<td><code>VERDASH</code><br/><code>REPORTEFINAL</code><br/><code>REPAVANCE</code></td><td>idPlanEstudio</td><td>Tablero y Compilador consolidado del rediseño:<br/><code>VERDASH</code><br/><code>REPORTEFINAL[\"1\"]</code></td></tr>");

        } else { // Docente Redactor / Invitado
            sb.append("<tr><td rowspan='3'><b>Redacción de Capítulos</b></td>")
                    .append("<td><code>LISCAP</code></td><td>idComponente (opcional)</td><td>Consultar capítulos asignados:<br/><code>LISCAP[\"1\"]</code></td></tr>")
                    .append("<tr><td><code>INSCAP</code></td><td>num, titulo, contenido, idComp</td><td>Registrar borrador de capítulo asignado:<br/><code>INSCAP[\"1\", \"Diagnostico Profesional\", \"Estudio de la demanda laboral...\", \"1\"]</code></td></tr>")
                    .append("<tr><td><code>UPDCAP</code></td><td>id, num, titulo, contenido, idComp</td><td>Actualizar contenido del capítulo:<br/><code>UPDCAP[\"1\", \"1\", \"Diagnostico Profesional v2\", \"Texto ampliado...\", \"1\"]</code></td></tr>");

            sb.append("<tr><td rowspan='2'><b>Subsanación Técnica</b></td>")
                    .append("<td><code>LISOBS</code></td><td>idCapitulo</td><td>Revisar observaciones emitidas:<br/><code>LISOBS[\"1\"]</code></td></tr>")
                    .append("<tr><td><code>SUBSANOBS</code></td><td>idObs, respuesta, idUsuario</td><td>Responder y subsanar formalmente:<br/><code>SUBSANOBS[\"1\", \"Se incorporaron las citas requeridas\", \"3\"]</code></td></tr>");

            sb.append("<tr><td><b>Evidencias de Respaldo</b></td>")
                    .append("<td><code>LISDOC</code> / <code>VINCULEVI</code></td><td>idCap, idDoc / criterio, valor</td><td>Vincular evidencias y resoluciones:<br/><code>VINCULEVI[\"1\", \"2\"]</code><br/><code>BUSDOC[\"TIPO\", \"Acta\"]</code></td></tr>");

            sb.append("<tr><td><b>Avance Curricular</b></td>")
                    .append("<td><code>REPAVANCE</code></td><td>idPlan</td><td>Monitorear avance del rediseño:<br/><code>REPAVANCE[\"1\"]</code></td></tr>");
        }

        sb.append("</table>");
        return sb.toString();
    }

    private String generarPerfilUsuarioHTML(Usuario u) {
        String rolColor = u.getIdRol() == 1 ? "#c0392b" : (u.getIdRol() == 2 ? "#2980b9" : "#27ae60");
        String rolBadge = "<span style='background-color: " + rolColor
                + "; color: white; padding: 4px 10px; border-radius: 4px; font-weight: bold;'>"
                + (u.getNombreRol() != null ? u.getNombreRol() : "Rol " + u.getIdRol()) + "</span>";

        String fotoHtml;
        String rutaFoto = u.getFotoRuta() != null ? u.getFotoRuta().trim() : "";
        if (rutaFoto.startsWith("http://") || rutaFoto.startsWith("https://")) {
            fotoHtml = "<img src='" + rutaFoto + "' alt='Foto' width='44' height='44' style='border-radius: 50%; object-fit: cover; border: 1px solid #ccc;'/>";
        } else {
            String iniciales = obtenerIniciales(u.getNombreCompleto());
            fotoHtml = "<div style='display: inline-block; width: 44px; height: 44px; line-height: 44px; border-radius: 50%; background-color: "
                    + rolColor + "; color: white; font-weight: bold; text-align: center; font-size: 16px;'>"
                    + iniciales + "</div>";
            if (!rutaFoto.isEmpty() && !rutaFoto.equalsIgnoreCase("fotos/default.png")) {
                fotoHtml += "<div style='font-size: 11px; color: #7f8c8d; margin-top: 2px;'>" + rutaFoto + "</div>";
            }
        }

        return "<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px; line-height: 1.5;'>" +
                "<div style='background-color: #003366; color: white; padding: 15px 20px; border-radius: 6px; max-width: 800px;'>" +
                "<h2 style='margin: 0;'>Inicio de Sesión Exitoso</h2>" +
                "<p style='margin: 4px 0 0 0; font-size: 13px; opacity: 0.9;'>Sistema de Seguimiento al Diseño Curricular - TecnoWeb UAGRM</p>" +
                "</div>" +
                "<div style='background-color: #f7f9fc; border: 1px solid #e1e8ed; padding: 20px; border-radius: 6px; margin-top: 15px; max-width: 800px;'>" +
                "<table cellpadding='8' style='width: 100%; border-collapse: collapse; font-size: 14px;'>" +
                "<tr><td style='width: 60px;'><b>Foto / Avatar:</b></td><td>" + fotoHtml + "</td></tr>" +
                "<tr><td style='width: 150px;'><b>Nombre Completo:</b></td><td><b>" + u.getNombreCompleto() + "</b></td></tr>" +
                "<tr><td><b>Nombre de Usuario:</b></td><td><code>" + u.getUsername() + "</code></td></tr>" +
                "<tr><td><b>Correo Vinculado:</b></td><td>" + u.getEmail() + "</td></tr>" +
                "<tr><td><b>Rol Asignado:</b></td><td>" + rolBadge + "</td></tr>" +
                "</table>" +
                "<hr style='border: 0; border-top: 1px solid #ddd; margin: 15px 0;'/>" +
                "<h3 style='margin: 0 0 8px 0; color: #003366;'>Comandos Habilitados y Ejemplos de Uso para su Rol:</h3>" +
                "<p style='margin: 0 0 10px 0; font-size: 13px; color: #555;'>Copie y pegue cualquiera de los siguientes comandos en el <b>Asunto (Subject)</b> de su correo:</p>" +
                generarTablaComandosRolHTML(u.getIdRol()) +
                "</div>" +
                "<p style='margin-top: 15px; font-size: 12px; color: #888;'>Su buzón ha quedado vinculado en el sistema. Todos los correos enviados desde esta dirección serán autorizados con sus privilegios.</p>" +
                "</body></html>";
    }

    private int resolverIdEstado(String estado) {
        String clean = estado.trim().toLowerCase();
        if (clean.contains("borrador"))
            return 1;
        if (clean.contains("revisión") || clean.contains("revision"))
            return 2;
        if (clean.contains("observado"))
            return 3;
        if (clean.contains("aprobado"))
            return 4;
        return -1;
    }

    public static String[] extraerParams(String subject) {
        if (subject == null) {
            return null;
        }

        int start = subject.indexOf('[');
        int end = subject.lastIndexOf(']');

        if (start < 0 || end < 0 || end <= start) {
            return null;
        }

        String inside = subject.substring(start + 1, end).trim();
        if (inside.isEmpty()) {
            return new String[0];
        }

        List<String> list = new ArrayList<>();
        Matcher matcher = PARAM_PATTERN.matcher(inside);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                list.add(matcher.group(1).trim());
            } else if (matcher.group(2) != null) {
                list.add(matcher.group(2).trim());
            } else if (matcher.group(3) != null) {
                String val = matcher.group(3).trim();
                if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                    val = val.substring(1, val.length() - 1).trim();
                } else if (val.startsWith("'") && val.endsWith("'") && val.length() >= 2) {
                    val = val.substring(1, val.length() - 1).trim();
                }
                if (!val.isEmpty()) {
                    list.add(val);
                }
            }
        }

        return list.toArray(new String[0]);
    }

    private String generarDashboardHTML(ReporteDAO.MetricasDashboard m) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<div style='background-color: #003366; color: white; padding: 15px; border-radius: 6px;'>")
                .append("<h2 style='margin: 0;'>Tablero de Control - Avance del Rediseño Curricular (RF-09)</h2>")
                .append("<p style='margin: 4px 0 0 0; font-size: 13px;'>Monitoreo métrico y alertas de capítulos observados</p>")
                .append("</div>");

        // Barra y porcentaje de avance
        String colorBarra = m.porcentajeAvance >= 80 ? "#27ae60" : (m.porcentajeAvance >= 50 ? "#2980b9" : "#e67e22");
        sb.append(
                "<div style='background-color: #f7f9fc; border: 1px solid #e1e8ed; padding: 15px; border-radius: 6px; margin: 15px 0;'>")
                .append("<div style='display: flex; justify-content: space-between; align-items: center;'>")
                .append("<h3 style='margin: 0; color: #003366;'>Avance Global del Documento Oficial: <span style='color: ")
                .append(colorBarra).append(";'>")
                .append(String.format("%.1f", m.porcentajeAvance)).append("%</span></h3>")
                .append("</div>")
                .append("<div style='background-color: #e0e0e0; border-radius: 8px; height: 16px; width: 100%; margin-top: 10px; overflow: hidden;'>")
                .append("<div style='background-color: ").append(colorBarra).append("; width: ")
                .append((int) m.porcentajeAvance).append("%; height: 100%;'></div>")
                .append("</div>")
                .append("<p style='margin: 6px 0 0 0; font-size: 12px; color: #666;'>").append(m.aprobadoCount)
                .append(" de ").append(m.totalCapitulos).append(" capítulos aprobados formalmente</p>")
                .append("</div>");

        // Tarjetas métricas de estado
        sb.append(
                "<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; text-align: center; margin-bottom: 20px; border-color: #ddd;'>")
                .append("<tr style='background-color: #f2f5f9; font-size: 13px;'>")
                .append("<th style='color: #27ae60;'>Aprobados</th>")
                .append("<th style='color: #2980b9;'>En Revisión</th>")
                .append("<th style='color: #c0392b;'>Observados</th>")
                .append("<th style='color: #7f8c8d;'>Borrador</th>")
                .append("<th style='color: #003366;'>Materias en Malla</th>")
                .append("<th style='color: #003366;'>Créditos Totales</th>")
                .append("</tr>")
                .append("<tr style='font-size: 18px; font-weight: bold;'>")
                .append("<td>").append(m.aprobadoCount).append("</td>")
                .append("<td>").append(m.enRevisionCount).append("</td>")
                .append("<td style='color: #c0392b;'>").append(m.observadoCount).append("</td>")
                .append("<td>").append(m.borradorCount).append("</td>")
                .append("<td>").append(m.totalAsignaturas).append("</td>")
                .append("<td>").append(m.totalCreditos).append("</td>")
                .append("</tr></table>");

        // Alerta de Secciones Observadas
        if (!m.capitulosObservados.isEmpty()) {
            sb.append(
                    "<div style='background-color: #fceae8; border-left: 5px solid #c0392b; padding: 12px; margin-bottom: 20px;'>")
                    .append("<h4 style='margin: 0; color: #c0392b;'>ALERTA: Secciones Observadas Pendientes de Subsanación (")
                    .append(m.capitulosObservados.size()).append(")</h4>")
                    .append("<p style='margin: 5px 0 10px 0; font-size: 12px;'>Los siguientes capítulos no podrán ser aprobados hasta que la Delegado responda y cierre los señalamientos:</p>")
                    .append("<table border='1' cellpadding='6' style='border-collapse: collapse; width: 100%; font-size: 12px; border-color: #ddd;'>")
                    .append("<tr style='background-color: #f9d5d1; color: #900;'><th>ID</th><th>Capítulo</th><th>Componente</th><th>Acción</th></tr>");

            for (Capitulo c : m.capitulosObservados) {
                sb.append("<tr>")
                        .append("<td align='center'>").append(c.getId()).append("</td>")
                        .append("<td><b>Capítulo ").append(c.getNumero()).append(": ").append(c.getTitulo())
                        .append("</b></td>")
                        .append("<td>").append(c.getNombreComponente()).append("</td>")
                        .append("<td align='center'><code>LISOBS[\"").append(c.getId()).append("\"]</code></td>")
                        .append("</tr>");
            }
            sb.append("</table></div>");
        } else {
            sb.append(
                    "<div style='background-color: #eafaf1; border-left: 5px solid #27ae60; padding: 10px; margin-bottom: 20px;'>")
                    .append("<p style='margin: 0; color: #27ae60; font-weight: bold;'>Excelente: No existen capítulos con observaciones pendientes en este momento.</p>")
                    .append("</div>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private String generarReporteFinalHTML(ReporteDAO.ReporteFinalConsolidado rep) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 25px; line-height: 1.6;'>")
                // Portada institucional
                .append("<div style='border-bottom: 3px solid #003366; padding-bottom: 15px; margin-bottom: 20px;'>")
                .append("<h3 style='margin: 0; color: #7f8c8d; text-transform: uppercase;'>Universidad Autónoma Gabriel René Moreno</h3>")
                .append("<h4 style='margin: 3px 0 0 0; color: #7f8c8d;'>Facultad de Ciencias Exactas y Tecnología</h4>")
                .append("<h1 style='margin: 10px 0 5px 0; color: #003366;'>Documento Oficial de Rediseño Curricular (RF-15)</h1>")
                .append("<p style='margin: 0; font-size: 14px;'>Carrera: <b>").append(rep.plan.getCarrera())
                .append("</b> | Plan: <b>").append(rep.plan.getCodigo())
                .append("</b> | Año: <b>").append(rep.plan.getAnioVigencia()).append("</b> | Estado: <b>")
                .append(rep.plan.getEstado()).append("</b></p>")
                .append("</div>");

        // 1. Marco Filosófico y Perfil de Egreso
        sb.append(
                "<h2 style='color: #003366; border-bottom: 1px solid #ddd; padding-bottom: 5px;'>1. Objetivos del Plan y Perfil de Egreso</h2>")
                .append("<h4>Objetivos de la Carrera:</h4>")
                .append("<p style='background-color: #f7f9fc; padding: 10px; border-left: 4px solid #003366;'>")
                .append(rep.plan.getObjetivos()).append("</p>")
                .append("<h4>Perfil de Egreso:</h4>")
                .append("<p style='background-color: #f7f9fc; padding: 10px; border-left: 4px solid #003366;'>")
                .append(rep.plan.getPerfilEgreso()).append("</p>")
                .append("<p><b>Límites de Créditos Formativos:</b> Mínimo ").append(rep.plan.getLimiteCreditosMin())
                .append(" - Máximo ").append(rep.plan.getLimiteCreditosMax()).append(" créditos.</p>");

        // 2. Malla Curricular Consolidada
        sb.append(
                "<h2 style='color: #003366; border-bottom: 1px solid #ddd; padding-bottom: 5px; margin-top: 30px;'>2. Malla Curricular Oficial</h2>")
                .append("<p>Total Asignaturas: <b>").append(rep.asignaturas.size())
                .append("</b> | Total Créditos Consolidados: <b>").append(rep.totalCreditos).append(" créditos</b></p>")
                .append("<table border='1' cellpadding='6' style='border-collapse: collapse; width: 100%; font-size: 12px; border-color: #ddd;'>")
                .append("<tr style='background-color: #003366; color: white;'>")
                .append("<th>Sem</th><th>Sigla</th><th>Nombre de Asignatura</th><th>HT</th><th>HP</th><th>Créditos</th><th>Área Formativa</th>")
                .append("</tr>");

        for (Asignatura a : rep.asignaturas) {
            sb.append("<tr>")
                    .append("<td align='center'><b>").append(a.getSemestre()).append("</b></td>")
                    .append("<td><b>").append(a.getSigla()).append("</b></td>")
                    .append("<td>").append(a.getNombre()).append("</td>")
                    .append("<td align='center'>").append(a.getHorasTeoricas()).append("</td>")
                    .append("<td align='center'>").append(a.getHorasPracticas()).append("</td>")
                    .append("<td align='center'><b>").append(a.getCreditos()).append("</b></td>")
                    .append("<td>").append(a.getAreaFormativa()).append("</td>")
                    .append("</tr>");
        }
        sb.append("</table>");

        // 3. Capítulos Aprobados Oficiales
        sb.append(
                "<h2 style='color: #003366; border-bottom: 1px solid #ddd; padding-bottom: 5px; margin-top: 30px;'>3. Capítulos Oficiales Aprobados</h2>");
        if (rep.capitulosAprobados.isEmpty()) {
            sb.append(
                    "<p style='color: #c0392b; font-style: italic;'>Aún no existen capítulos en estado 'Aprobado' para compilar formalmente.</p>");
        } else {
            for (Capitulo c : rep.capitulosAprobados) {
                sb.append("<div style='margin-bottom: 25px;'>")
                        .append("<h3 style='color: #2c3e50; margin-bottom: 4px;'>Capítulo ").append(c.getNumero())
                        .append(": ").append(c.getTitulo()).append("</h3>")
                        .append("<p style='font-size: 12px; color: #7f8c8d; margin-top: 0;'>Componente: <b>")
                        .append(c.getNombreComponente()).append("</b> | Redactores: <b>").append(c.getResponsables())
                        .append("</b> | Estado: <b style='color: green;'>Aprobado</b></p>")
                        .append("<div style='background-color: #fafafa; border: 1px solid #eee; padding: 12px; border-radius: 4px;'>")
                        .append("<p style='margin: 0;'>")
                        .append(c.getContenido() != null ? c.getContenido() : "<i>Texto en custodia institucional</i>")
                        .append("</p>")
                        .append("</div></div>");
            }
        }

        // 4. Anexos Normativos Vigentes
        sb.append(
                "<h2 style='color: #003366; border-bottom: 1px solid #ddd; padding-bottom: 5px; margin-top: 30px;'>4. Anexos de Resoluciones y Evidencias Normativas Vigentes</h2>")
                .append("<table border='1' cellpadding='6' style='border-collapse: collapse; width: 100%; font-size: 12px; border-color: #ddd;'>")
                .append("<tr style='background-color: #f2f5f9; color: #003366;'>")
                .append("<th>Código Norma</th><th>Título de la Resolución</th><th>Tipo</th><th>Órgano Emisor</th><th>Fecha de Emisión</th><th>Archivo de Respaldo</th>")
                .append("</tr>");

        for (DocumentoEvidencia d : rep.evidenciasVigentes) {
            sb.append("<tr>")
                    .append("<td><b>").append(d.getCodigoNorma()).append("</b></td>")
                    .append("<td>").append(d.getTitulo()).append("</td>")
                    .append("<td align='center'>").append(d.getTipoDocumento()).append("</td>")
                    .append("<td>").append(d.getOrganoEmisor()).append("</td>")
                    .append("<td align='center'>").append(d.getFechaEmision()).append("</td>")
                    .append("<td style='color: #2980b9;'>").append(d.getArchivoRuta()).append("</td>")
                    .append("</tr>");
        }
        sb.append("</table>");

        sb.append(
                "<p style='margin-top: 30px; font-size: 11px; color: #999; text-align: center;'>Documento generado automáticamente por el Sistema de Seguimiento Curricular - TecnoWeb UAGRM</p>")
                .append("</body></html>");
        return sb.toString();
    }

    private String generarReporteAvanceHTML(List<ReporteDAO.EstadisticaComponente> comps) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<h2 style='color: #003366;'>Reporte de Avance por Componente Curricular (CU8)</h2>")
                .append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd;'>")
                .append("<tr style='background-color: #003366; color: white;'>")
                .append("<th>N°</th><th>Componente Temático</th><th>Total Capítulos</th><th>Aprobados</th><th>% Cumplimiento</th><th>Barra de Avance</th>")
                .append("</tr>");

        for (ReporteDAO.EstadisticaComponente c : comps) {
            String color = c.porcentaje >= 80 ? "#27ae60" : (c.porcentaje >= 40 ? "#2980b9" : "#e67e22");
            sb.append("<tr>")
                    .append("<td align='center'><b>").append(c.numero).append("</b></td>")
                    .append("<td><b>").append(c.nombre).append("</b></td>")
                    .append("<td align='center'>").append(c.totalCapitulos).append("</td>")
                    .append("<td align='center'><b>").append(c.aprobados).append("</b></td>")
                    .append("<td align='center'><b style='color: ").append(color).append(";'>")
                    .append(String.format("%.1f", c.porcentaje)).append("%</b></td>")
                    .append("<td><div style='background-color: #eee; width: 100%; height: 12px; border-radius: 4px; overflow: hidden;'>")
                    .append("<div style='background-color: ").append(color).append("; width: ")
                    .append((int) c.porcentaje).append("%; height: 100%;'></div>")
                    .append("</div></td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarReporteUsuariosHTML(List<ReporteDAO.EstadisticaUsuario> usuarios) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<h2 style='color: #003366;'>Reporte de Productividad y Asignaciones por Usuario (CU8)</h2>")
                .append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd;'>")
                .append("<tr style='background-color: #003366; color: white;'>")
                .append("<th>ID</th><th>Nombre del Miembro</th><th>Rol</th><th>Capítulos Asignados</th><th>Observaciones Emitidas</th><th>Subsanaciones Resueltas</th>")
                .append("</tr>");

        for (ReporteDAO.EstadisticaUsuario u : usuarios) {
            sb.append("<tr>")
                    .append("<td align='center'>").append(u.idUsuario).append("</td>")
                    .append("<td><b>").append(u.nombreCompleto).append("</b></td>")
                    .append("<td align='center'>").append(u.rolNombre).append("</td>")
                    .append("<td align='center'><b>").append(u.capitulosAsignados).append("</b></td>")
                    .append("<td align='center'>").append(u.observacionesEmitidas).append("</td>")
                    .append("<td align='center'><b style='color: green;'>").append(u.observacionesSubsanadas)
                    .append("</b></td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarTablaUsuariosHTML(List<Usuario> usuarios) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<div style='background-color: #003366; color: white; padding: 12px 15px; border-radius: 6px;'>")
                .append("<h2 style='margin: 0;'>Listado de Usuarios Registrados</h2>")
                .append("<p style='margin: 4px 0 0 0; font-size: 13px;'>Gestión de Usuarios y Roles del Sistema Curricular (CU1)</p>")
                .append("</div>")
                .append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd; margin-top: 15px;'>")
                .append("<tr style='background-color: #f2f5f9; color: #003366;'>")
                .append("<th>ID</th><th>Foto / Avatar</th><th>Nombre Completo</th><th>Email</th><th>Usuario</th><th>Rol</th><th>Estado</th>")
                .append("</tr>");

        for (Usuario u : usuarios) {
            String rolColor = u.getIdRol() == 1 ? "#c0392b" : (u.getIdRol() == 2 ? "#2980b9" : "#27ae60");
            String rolBadge = "<span style='background-color: " + rolColor
                    + "; color: white; padding: 3px 8px; border-radius: 3px; font-weight: bold; font-size: 11px;'>"
                    + (u.getNombreRol() != null ? u.getNombreRol() : "Rol " + u.getIdRol()) + "</span>";

            String fotoHtml;
            String rutaFoto = u.getFotoRuta() != null ? u.getFotoRuta().trim() : "";
            if (rutaFoto.startsWith("http://") || rutaFoto.startsWith("https://")) {
                fotoHtml = "<img src='" + rutaFoto + "' alt='Foto' width='36' height='36' style='border-radius: 50%; object-fit: cover; border: 1px solid #ccc;'/>";
            } else {
                String iniciales = obtenerIniciales(u.getNombreCompleto());
                fotoHtml = "<div style='display: inline-block; width: 36px; height: 36px; line-height: 36px; border-radius: 50%; background-color: "
                        + rolColor + "; color: white; font-weight: bold; text-align: center; font-size: 13px;'>"
                        + iniciales + "</div>";
                if (!rutaFoto.isEmpty() && !rutaFoto.equalsIgnoreCase("fotos/default.png")) {
                    fotoHtml += "<div style='font-size: 10px; color: #7f8c8d; margin-top: 2px;'>" + rutaFoto + "</div>";
                }
            }

            sb.append("<tr>")
                    .append("<td align='center'><b>").append(u.getId()).append("</b></td>")
                    .append("<td align='center'>").append(fotoHtml).append("</td>")
                    .append("<td><b>").append(u.getNombreCompleto()).append("</b></td>")
                    .append("<td>").append(u.getEmail()).append("</td>")
                    .append("<td><code>").append(u.getUsername()).append("</code></td>")
                    .append("<td align='center'>").append(rolBadge).append("</td>")
                    .append("<td align='center'>")
                    .append(u.isActivo() ? "<span style='color: #27ae60; font-weight: bold;'>Activo</span>"
                            : "<span style='color: #c0392b; font-weight: bold;'>Inactivo</span>")
                    .append("</td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarTablaPlanesHTML(List<PlanEstudio> planes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<h2 style='color: #003366;'>Planes de Estudio Curriculares</h2>")
                .append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd;'>")
                .append("<tr style='background-color: #003366; color: white;'>")
                .append("<th>ID</th><th>Código</th><th>Carrera</th><th>Año</th><th>Créditos (Mín - Máx)</th><th>Estado</th>")
                .append("</tr>");

        for (PlanEstudio p : planes) {
            sb.append("<tr>")
                    .append("<td align='center'>").append(p.getId()).append("</td>")
                    .append("<td><b>").append(p.getCodigo()).append("</b></td>")
                    .append("<td>").append(p.getCarrera()).append("</td>")
                    .append("<td align='center'>").append(p.getAnioVigencia()).append("</td>")
                    .append("<td align='center'>").append(p.getLimiteCreditosMin()).append(" - ")
                    .append(p.getLimiteCreditosMax()).append("</td>")
                    .append("<td align='center'><b>").append(p.getEstado()).append("</b></td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarTablaAsignaturasHTML(List<Asignatura> materias) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<h2 style='color: #003366;'>Malla Curricular: Listado de Asignaturas</h2>")
                .append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd;'>")
                .append("<tr style='background-color: #003366; color: white;'>")
                .append("<th>ID</th><th>Sigla</th><th>Nombre de Asignatura</th><th>Semestre</th><th>HT</th><th>HP</th><th>Créditos</th><th>Área Formativa</th><th>Plan ID</th>")
                .append("</tr>");

        for (Asignatura a : materias) {
            sb.append("<tr>")
                    .append("<td align='center'>").append(a.getId()).append("</td>")
                    .append("<td><b>").append(a.getSigla()).append("</b></td>")
                    .append("<td>").append(a.getNombre()).append("</td>")
                    .append("<td align='center'><b>").append(a.getSemestre()).append("</b></td>")
                    .append("<td align='center'>").append(a.getHorasTeoricas()).append("</td>")
                    .append("<td align='center'>").append(a.getHorasPracticas()).append("</td>")
                    .append("<td align='center'><b>").append(a.getCreditos()).append("</b></td>")
                    .append("<td>").append(a.getAreaFormativa()).append("</td>")
                    .append("<td align='center'>").append(a.getIdPlanEstudio()).append("</td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarTablaPrerrequisitosDirectosHTML(int idAsig, List<Prerrequisito> lista) {
        StringBuilder sb = new StringBuilder();
        String materiaPrincipal = lista.get(0).getSiglaAsignatura() + " - " + lista.get(0).getNombreAsignatura();

        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<h2 style='color: #003366;'>Prerrequisitos Directos de la Asignatura</h2>")
                .append("<p style='font-size: 14px;'>Asignatura Evaluada: <b>").append(materiaPrincipal)
                .append("</b> (ID: ").append(idAsig).append(")</p>")
                .append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd;'>")
                .append("<tr style='background-color: #003366; color: white;'>")
                .append("<th>ID Requisito</th><th>Sigla Requisito</th><th>Nombre Asignatura Requisito</th><th>Semestre Requisito</th>")
                .append("</tr>");

        for (Prerrequisito p : lista) {
            sb.append("<tr>")
                    .append("<td align='center'>").append(p.getIdAsignaturaRequisito()).append("</td>")
                    .append("<td><b>").append(p.getSiglaRequisito()).append("</b></td>")
                    .append("<td>").append(p.getNombreRequisito()).append("</td>")
                    .append("<td align='center'><b>").append(p.getSemestreRequisito()).append("</b></td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarVistaGrafoHTML(Asignatura actual, List<Asignatura> haciaAtras,
            List<Asignatura> haciaAdelante) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<div style='background-color: #003366; color: white; padding: 12px; border-radius: 6px;'>")
                .append("<h2 style='margin: 0;'>Grafo Curricular de Prelación (RF-04)</h2>")
                .append("<p style='margin: 4px 0 0 0;'>Trazabilidad hacia atrás (requisitos previos) y hacia adelante (materias dependientes)</p>")
                .append("</div>");

        sb.append(
                "<div style='background-color: #eef4fb; border-left: 5px solid #003366; padding: 12px; margin: 15px 0;'>")
                .append("<h3 style='margin: 0; color: #003366;'>Materia Central: [").append(actual.getSigla())
                .append("] ").append(actual.getNombre()).append("</h3>")
                .append("<p style='margin: 5px 0 0 0; font-size: 13px;'>Semestre: <b>").append(actual.getSemestre())
                .append("</b> | Créditos: <b>").append(actual.getCreditos())
                .append("</b> | Área: <b>").append(actual.getAreaFormativa())
                .append("</b> | ID: <b>").append(actual.getId()).append("</b></p>")
                .append("</div>");

        sb.append(
                "<h4 style='color: #c0392b; margin-bottom: 8px;'>← 1. Cadena de Prerrequisitos Hacia Atrás (Materias que debe cursar antes):</h4>");
        if (haciaAtras.isEmpty()) {
            sb.append(
                    "<p style='font-size: 13px; color: #7f8c8d; font-style: italic;'>Esta materia no tiene ningún prerrequisito previo en la malla (Materia de base o Semestre 1).</p>");
        } else {
            sb.append(
                    "<table border='1' cellpadding='6' style='border-collapse: collapse; width: 100%; font-size: 12px; border-color: #ddd; margin-bottom: 20px;'>")
                    .append("<tr style='background-color: #fceae8; color: #c0392b;'>")
                    .append("<th>ID</th><th>Sigla</th><th>Asignatura Requisito</th><th>Semestre</th><th>Créditos</th>")
                    .append("</tr>");
            for (Asignatura a : haciaAtras) {
                sb.append("<tr>")
                        .append("<td align='center'>").append(a.getId()).append("</td>")
                        .append("<td><b>").append(a.getSigla()).append("</b></td>")
                        .append("<td>").append(a.getNombre()).append("</td>")
                        .append("<td align='center'><b>Semestre ").append(a.getSemestre()).append("</b></td>")
                        .append("<td align='center'>").append(a.getCreditos()).append("</td>")
                        .append("</tr>");
            }
            sb.append("</table>");
        }

        sb.append(
                "<h4 style='color: #27ae60; margin-bottom: 8px;'>→ 2. Cadena de Asignaturas Dependientes Hacia Adelante (Materias que desbloquea):</h4>");
        if (haciaAdelante.isEmpty()) {
            sb.append(
                    "<p style='font-size: 13px; color: #7f8c8d; font-style: italic;'>Ninguna otra materia de la malla tiene a esta asignatura como prerrequisito (Materia terminal o de último semestre).</p>");
        } else {
            sb.append(
                    "<table border='1' cellpadding='6' style='border-collapse: collapse; width: 100%; font-size: 12px; border-color: #ddd;'>")
                    .append("<tr style='background-color: #eafaf1; color: #27ae60;'>")
                    .append("<th>ID</th><th>Sigla</th><th>Asignatura Dependiente</th><th>Semestre</th><th>Créditos</th>")
                    .append("</tr>");
            for (Asignatura a : haciaAdelante) {
                sb.append("<tr>")
                        .append("<td align='center'>").append(a.getId()).append("</td>")
                        .append("<td><b>").append(a.getSigla()).append("</b></td>")
                        .append("<td>").append(a.getNombre()).append("</td>")
                        .append("<td align='center'><b>Semestre ").append(a.getSemestre()).append("</b></td>")
                        .append("<td align='center'>").append(a.getCreditos()).append("</td>")
                        .append("</tr>");
            }
            sb.append("</table>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private String generarTablaComponentesHTML(List<Componente> lista) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<h2 style='color: #003366;'>Componentes del Marco Curricular</h2>")
                .append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd;'>")
                .append("<tr style='background-color: #003366; color: white;'>")
                .append("<th>ID</th><th>N°</th><th>Nombre del Componente</th><th>Descripción</th><th>Plan ID</th>")
                .append("</tr>");

        for (Componente c : lista) {
            sb.append("<tr>")
                    .append("<td align='center'>").append(c.getId()).append("</td>")
                    .append("<td align='center'><b>").append(c.getNumero()).append("</b></td>")
                    .append("<td><b>").append(c.getNombre()).append("</b></td>")
                    .append("<td>").append(c.getDescripcion() != null ? c.getDescripcion() : "-").append("</td>")
                    .append("<td align='center'>").append(c.getIdPlanEstudio()).append("</td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarTablaCapitulosHTML(List<Capitulo> caps) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<h2 style='color: #003366;'>Estructura del Rediseño: Capítulos y Responsables</h2>")
                .append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd;'>")
                .append("<tr style='background-color: #003366; color: white;'>")
                .append("<th>ID</th><th>N°</th><th>Título del Capítulo</th><th>Componente</th><th>Estado</th><th>Responsables de Redacción</th>")
                .append("</tr>");

        for (Capitulo c : caps) {
            String colorEstado = "#7f8c8d";
            if ("Aprobado".equalsIgnoreCase(c.getNombreEstado()))
                colorEstado = "#27ae60";
            else if ("En Revisión".equalsIgnoreCase(c.getNombreEstado()))
                colorEstado = "#2980b9";
            else if ("Observado".equalsIgnoreCase(c.getNombreEstado()))
                colorEstado = "#c0392b";

            sb.append("<tr>")
                    .append("<td align='center'>").append(c.getId()).append("</td>")
                    .append("<td align='center'><b>").append(c.getNumero()).append("</b></td>")
                    .append("<td><b>").append(c.getTitulo()).append("</b></td>")
                    .append("<td>")
                    .append(c.getNombreComponente() != null ? c.getNombreComponente() : c.getIdComponente())
                    .append("</td>")
                    .append("<td align='center'><b style='color: ").append(colorEstado).append(";'>")
                    .append(c.getNombreEstado()).append("</b></td>")
                    .append("<td>").append(c.getResponsables()).append("</td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarTablaObservacionesHTML(int idCap, List<Observacion> lista) {
        StringBuilder sb = new StringBuilder();
        String titulo = lista.get(0).getTituloCapitulo() != null ? lista.get(0).getTituloCapitulo()
                : "Capítulo #" + idCap;

        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<h2 style='color: #003366;'>Observaciones Técnicas del Capítulo</h2>")
                .append("<p style='font-size: 14px;'>Capítulo Evaluado: <b>").append(titulo).append("</b> (ID: ")
                .append(idCap).append(")</p>")
                .append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd;'>")
                .append("<tr style='background-color: #003366; color: white;'>")
                .append("<th>ID</th><th>Fecha</th><th>Autoridad (Observador)</th><th>Observación Técnica</th><th>Estado</th><th>Delegado (Subsanador)</th><th>Respuesta de Subsanación</th>")
                .append("</tr>");

        for (Observacion o : lista) {
            String badge = o.isResuelta()
                    ? "<span style='color: #27ae60; font-weight: bold;'>Subsanada</span>"
                    : "<span style='color: #c0392b; font-weight: bold;'>Pendiente</span>";

            sb.append("<tr>")
                    .append("<td align='center'>").append(o.getId()).append("</td>")
                    .append("<td style='font-size: 11px;'>").append(o.getFechaCreacion()).append("</td>")
                    .append("<td><b>")
                    .append(o.getNombreObservador() != null ? o.getNombreObservador() : o.getIdUsuarioObservador())
                    .append("</b></td>")
                    .append("<td>").append(o.getComentario()).append("</td>")
                    .append("<td align='center'>").append(badge).append("</td>")
                    .append("<td>").append(o.getNombreRespuesta() != null ? o.getNombreRespuesta() : "-")
                    .append("</td>")
                    .append("<td>")
                    .append(o.getRespuestaDelegado() != null ? o.getRespuestaDelegado()
                            : "<i style='color: #888;'>Sin respuesta</i>")
                    .append("</td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarTablaHistorialHTML(int idCap, List<HistorialModificacion> lista) {
        StringBuilder sb = new StringBuilder();
        String titulo = lista.get(0).getTituloCapitulo() != null ? lista.get(0).getTituloCapitulo()
                : "Capítulo #" + idCap;

        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<div style='background-color: #003366; color: white; padding: 12px; border-radius: 6px;'>")
                .append("<h2 style='margin: 0;'>Pista de Auditoría Histórica (RF-10)</h2>")
                .append("<p style='margin: 4px 0 0 0;'>Registro inmutable de cambios de estado para: <b>")
                .append(titulo).append("</b> (ID: ").append(idCap).append(")</p>")
                .append("</div>")
                .append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd; margin-top: 15px;'>")
                .append("<tr style='background-color: #f2f5f9; color: #003366;'>")
                .append("<th>ID</th><th>Fecha y Hora</th><th>Estado Anterior</th><th>Estado Nuevo</th><th>Justificación Técnica</th><th>Usuario Responsable</th>")
                .append("</tr>");

        for (HistorialModificacion h : lista) {
            sb.append("<tr>")
                    .append("<td align='center'>").append(h.getId()).append("</td>")
                    .append("<td style='font-size: 11px;'>").append(h.getFechaHora()).append("</td>")
                    .append("<td align='center'><b>").append(h.getEstadoAnterior()).append("</b></td>")
                    .append("<td align='center'><b style='color: #2980b9;'>").append(h.getEstadoNuevo())
                    .append("</b></td>")
                    .append("<td>").append(h.getJustificacionTecnica()).append("</td>")
                    .append("<td><b>").append(h.getNombreUsuario() != null ? h.getNombreUsuario() : h.getIdUsuario())
                    .append("</b></td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarTablaDocumentosHTML(List<DocumentoEvidencia> docs, String tituloSeccion) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>\n")
                .append("<div style='background-color: #003366; color: white; padding: 12px; border-radius: 6px;'>\n")
                .append("<h2 style='margin: 0;'>").append(tituloSeccion).append("</h2>\n")
                .append("<p style='margin: 4px 0 0 0;'>Repositorio Digital de Evidencias Normativas (RF-11 a RF-14)</p>\n")
                .append("</div>\n")
                .append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd; margin-top: 15px;'>\n")
                .append("<tr style='background-color: #f2f5f9; color: #003366;'>\n")
                .append("<th>ID</th><th>Código Norma</th><th>Título / Descripción</th><th>Tipo</th><th>Órgano Emisor</th><th>Emisión</th><th>Archivo / Enlace de Respaldo</th><th>Vigencia</th><th>Norma Padre</th>\n")
                .append("</tr>\n");

        for (DocumentoEvidencia d : docs) {
            String vigenciaBadge = d.isVigente()
                    ? "<span style='color: #27ae60; font-weight: bold;'>Vigente</span>"
                    : "<span style='color: #c0392b; font-weight: bold;'>Derogada/Obs</span>";

            String padreInfo = d.getCodigoNormaPadre() != null ? d.getCodigoNormaPadre() : "-";

            String enlaceHtml;
            String ruta = d.getArchivoRuta() != null ? d.getArchivoRuta().trim() : "";
            if (ruta.startsWith("http://") || ruta.startsWith("https://")) {
                enlaceHtml = "<a href='" + ruta + "' target='_blank' style='display:inline-block; background-color:#2980b9; color:#ffffff; padding:4px 10px; border-radius:4px; text-decoration:none; font-weight:bold; font-size:11px;'>📥 Ver / Descargar Documento</a>";
            } else {
                enlaceHtml = "<span style='display:inline-block; background-color:#ecf0f1; color:#2c3e50; padding:3px 6px; border-radius:3px; font-size:11px; font-family:monospace;'>📁 "
                        + (ruta.isEmpty() ? "storage/documentos/sin_archivo.pdf" : ruta) + "</span>";
            }

            sb.append("<tr>\n")
                    .append("<td align='center'>").append(d.getId()).append("</td>\n")
                    .append("<td><b>").append(d.getCodigoNorma()).append("</b></td>\n")
                    .append("<td>").append(d.getTitulo()).append("</td>\n")
                    .append("<td align='center'>").append(d.getTipoDocumento()).append("</td>\n")
                    .append("<td>").append(d.getOrganoEmisor()).append("</td>\n")
                    .append("<td align='center'>").append(d.getFechaEmision()).append("</td>\n")
                    .append("<td align='center'>").append(enlaceHtml).append("</td>\n")
                    .append("<td align='center'>").append(vigenciaBadge).append("</td>\n")
                    .append("<td align='center'>").append(padreInfo).append("</td>\n")
                    .append("</tr>\n");
        }

        sb.append("</table>\n</body></html>\n");
        return sb.toString();
    }

    private String generarManualHTML() {
        return "<html><body style='font-family: Arial, sans-serif; color: #333; line-height: 1.5; margin: 20px;'>" +
                "<div style='background-color: #003366; color: white; padding: 15px; border-radius: 6px;'>" +
                "<h2 style='margin: 0;'>Sistema de Seguimiento al Diseño Curricular</h2>" +
                "<p style='margin: 5px 0 0 0; font-size: 14px;'>Grupo 02 SA - Universidad Autónoma Gabriel René Moreno</p>"
                +
                "</div>" +
                "<h3 style='color: #003366; margin-top: 25px;'>Manual de Comandos del Sistema</h3>" +
                "<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd;'>"
                +
                "<tr style='background-color: #f2f5f9; color: #003366;'>" +
                "<th>Módulo / Área Funcional</th><th>Comando de Consulta</th><th>Comando de Inserción / Acción</th>" +
                "</tr>" +
                "<tr><td><b>Acceso y Sesión</b></td><td><code>LOGIN[\"email\",\"pass\"]</code></td><td>Inicia sesión y vincula el correo para autorizar comandos por rol.</td></tr>"
                +
                "<tr><td><b>Gestión de Usuarios</b></td><td><code>LISUSU[\"*\"]</code></td><td><code>INSUSU[\"nom\",\"email\",\"user\",\"pass\",\"idRol\",\"foto\"]</code><br/><code>UPDUSU[...]</code> / <code>DELUSU[\"id\"]</code></td></tr>"
                +
                "<tr><td><b>Planes de Estudio y Materias</b></td><td><code>LISPLA[\"*\"]</code><br/><code>LISASIG[\"idPlan\"]</code></td><td><code>INSPLA[\"cod\",\"carrera\",\"año\",\"desc\",\"obj\",\"perfil\",\"minCred\",\"maxCred\"]</code><br/><code>INSASIG[\"sigla\",\"nom\",\"sem\",\"ht\",\"hp\",\"cred\",\"area\",\"idPlan\"]</code></td></tr>"
                +
                "<tr><td><b>Prerrequisitos y Malla</b></td><td><code>LISPRE[\"idAsig\"]</code><br/><code>VERGRAFO[\"idAsig\"]</code></td><td><code>INSPRE[\"idAsig\",\"idAsigRequisito\"]</code><br/><code>DELPRE[\"idAsig\",\"idAsigRequisito\"]</code></td></tr>"
                +
                "<tr><td><b>Componentes y Capítulos</b></td><td><code>LISCOM[\"idPlan\"]</code><br/><code>LISCAP[\"idComp\"]</code></td><td><code>INSCOM[\"num\",\"nom\",\"desc\",\"idPlan\"]</code><br/><code>INSCAP[\"num\",\"titulo\",\"contenido\",\"idComp\"]</code><br/><code>ASIGCAP[\"idCap\",\"idUsu\"]</code></td></tr>"
                +
                "<tr><td><b>Flujo de Estados y Auditoría</b></td><td><code>LISOBS[\"idCap\"]</code><br/><code>LISHIST[\"idCap\"]</code></td><td><code>CAMBESTADO[\"idCap\",\"nuevoEstado\",\"justificacion\",\"idUsu\"]</code><br/><code>INSOBS[\"idCap\",\"comentario\",\"idAutoridad\"]</code><br/><code>SUBSANOBS[\"idObs\",\"respuesta\",\"idDelegado\"]</code></td></tr>"
                +
                "<tr><td><b>Evidencias Normativas</b></td><td><code>LISDOC[\"*\"]</code><br/><code>BUSDOC[\"criterio\",\"valor\"]</code></td><td><code>INSDOC[\"codNorma\",\"titulo\",\"tipo\",\"emisor\",\"fecha\",\"archivo\",\"idPadre\"]</code><br/><code>VINCULEVI[\"idCap\",\"idDoc\"]</code></td></tr>"
                +
                "<tr><td><b>Monitoreo y Compilador</b></td><td><code>VERDASH[\"*\"]</code></td><td><code>REPORTEFINAL[\"idPlan\"]</code> (Compilador consolidado oficial)</td></tr>"
                +
                "<tr><td><b>Reportes y Estadísticas</b></td><td><code>REPAVANCE[\"*\"]</code><br/><code>REPUSUARIOS[\"*\"]</code></td><td><code>HELP[\"*\"]</code> (Manual de comandos)</td></tr>"
                +
                "</table>" +
                "<div style='margin-top: 25px; padding: 20px; border: 1px solid #b2dfdb; background-color: #f4fbf9; border-radius: 6px; border-left: 5px solid #00897b;'>" +
                "<h3 style='margin: 0 0 14px 0; color: #004d40; font-size: 16px;'>📎 Guía Práctica: Cómo Cargar Fotos y Documentos PDF</h3>" +
                "<div style='background-color: #ffffff; border: 1px solid #e0f2f1; padding: 14px; border-radius: 4px; margin-bottom: 12px;'>" +
                "<h4 style='margin: 0 0 6px 0; color: #00695c; font-size: 14px;'>1. Fotos de Perfil de Usuario (Comandos INSUSU / UPDUSU):</h4>" +
                "<p style='margin: 0; font-size: 13px; color: #444; line-height: 1.5;'>" +
                "• <b>Enlace Web (Recomendado):</b> Puede ingresar una URL pública hacia la imagen (ej: <code>https://i.imgur.com/usuario.png</code> o <code>https://i.pravatar.cc/150?u=croca</code>). En <code>LISUSU</code> se renderizará la foto real.<br/>" +
                "• <b>Avatar Automático:</b> Si coloca <code>\"fotos/default.png\"</code> o una ruta local vacía, el sistema generará automáticamente un avatar circular CSS con las iniciales del usuario y el color institucional de su rol." +
                "</p>" +
                "</div>" +
                "<div style='background-color: #ffffff; border: 1px solid #e0f2f1; padding: 14px; border-radius: 4px;'>" +
                "<h4 style='margin: 0 0 6px 0; color: #00695c; font-size: 14px;'>2. Documentos PDF de Respaldo y Evidencias (Comando INSDOC):</h4>" +
                "<p style='margin: 0; font-size: 13px; color: #444; line-height: 1.5;'>" +
                "• <b>Paso 1:</b> Suba su documento PDF a su cuenta de <b>Google Drive</b>, OneDrive o repositorio público.<br/>" +
                "• <b>Paso 2:</b> En Google Drive, haga clic derecho en el archivo ➔ <i>Compartir</i> ➔ Cambie el acceso general a <b>'Cualquier persona con el enlace (Lector)'</b> y copie el enlace.<br/>" +
                "• <b>Paso 3:</b> Pegue la URL completa en el parámetro <code>archivoRuta</code> del comando:<br/>" +
                "<code style='display: block; background: #eef7f6; padding: 8px 12px; border-radius: 3px; color: #004d40; margin: 6px 0; font-size: 12px;'>" +
                "INSDOC[\"RES-045/2026\", \"Aprobación del Nuevo Plan\", \"Resolucion\", \"Consejo Facultativo\", \"2026-03-15\", \"https://drive.google.com/file/d/1A2B3C4D.../view\", \"0\"]" +
                "</code>" +
                "• <b>Paso 4:</b> Al consultar con <code>LISDOC</code> o <code>BUSDOC</code>, el sistema incluirá un botón interactivo <b>[📥 Ver / Descargar Documento]</b> que abrirá el PDF de inmediato.<br/>" +
                "• <i>Almacenamiento Local:</i> Si prefiere almacenar físicamente el archivo en el servidor, colóquelo en la carpeta <code>storage/documentos/</code> e indique <code>storage/documentos/resolucion045.pdf</code>." +
                "</p>" +
                "</div>" +
                "</div>" +
                "<p style='margin-top: 20px; font-size: 12px; color: #666;'>* Envíe el comando exacto en el campo <b>Asunto (Subject)</b> de su correo.</p>"
                +
                "</body></html>";
    }
}
