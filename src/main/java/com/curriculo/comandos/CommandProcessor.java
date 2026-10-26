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
 * Soporta la totalidad de los 8 Casos de Uso (CU1 al CU8) y los 15 Requisitos Funcionales (RF-01 a RF-15).
 */
public class CommandProcessor {

    private static final Pattern PARAM_PATTERN = Pattern.compile("\\s*\"([^\"]*)\"\\s*|\\s*'([^']*)'\\s*|\\s*([^,\\[\\]]+)\\s*");

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
                    return "Error de sintaxis en LOGIN: Se esperaban 2 parámetros (email y password).\n" +
                            "Formato requerido: LOGIN[\"email\", \"password\"]\n" +
                            "Ejemplo: LOGIN[\"admin@uagrm.edu.bo\", \"" + PasswordValidator.EJEMPLO_VALIDO + "\"]";
                }
                String email = p[0].trim();
                String pass = p[1].trim();

                Usuario u = usuarioDAO.autenticar(email, pass);
                if (u == null) {
                    return "Acceso denegado: Credenciales incorrectas o usuario inactivo.\n" +
                            "Verifique su dirección de correo electrónico y contraseña e intente nuevamente.";
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
                    return "Error de sintaxis en INSUSU: Se esperaban al menos 5 parámetros.\n" +
                            "Formato requerido: INSUSU[\"Nombre Completo\", \"email\", \"username\", \"password\", \"idRol\", \"fotoRuta\"]\n" +
                            "Ejemplo: INSUSU[\"Lic. Carlos Roca\", \"croca@uagrm.edu.bo\", \"croca\", \"" + PasswordValidator.EJEMPLO_VALIDO + "\", \"3\", \"fotos/croca.png\"]";
                }
                String nombre = p[0].trim();
                String email = p[1].trim();
                String username = p[2].trim();
                String pass = p[3].trim();
                int idRol = parseEntero(p[4], "idRol (1=Admin, 2=Comisión, 3=Docente)");
                String foto = p.length >= 6 && !p[5].trim().isEmpty() ? p[5].trim() : "fotos/default.png";

                // Validación estricta de contraseña
                PasswordValidator.Resultado resPass = PasswordValidator.validar(pass);
                if (!resPass.isValido()) {
                    return resPass.getMensaje();
                }

                if (usuarioDAO.obtenerPorEmail(email) != null) {
                    return "Error al registrar usuario: La dirección de correo electrónico '" + email + "' ya se encuentra registrada en el sistema.";
                }
                if (usuarioDAO.obtenerPorUsername(username) != null) {
                    return "Error al registrar usuario: El nombre de usuario '" + username + "' ya está en uso. Elija otro.";
                }

                String passHash = PasswordHasher.hash(pass);
                Usuario u = new Usuario(nombre, email, username, passHash, foto, idRol);
                int idGen = usuarioDAO.insertar(u);
                return idGen > 0
                        ? "Usuario registrado exitosamente. ID asignado: " + idGen + " (Rol ID: " + idRol + ")"
                        : "Error al registrar usuario en la base de datos. Verifique que el idRol exista (1=Admin, 2=Comisión, 3=Docente).";
            }

            if (commandUpper.startsWith("UPDUSU")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 7) {
                    return "Error de sintaxis en UPDUSU: Se esperaban al menos 7 parámetros.\n" +
                            "Formato requerido: UPDUSU[\"id\", \"Nombre Completo\", \"email\", \"username\", \"password\", \"idRol\", \"fotoRuta\", \"activo\"]\n" +
                            "Ejemplo: UPDUSU[\"1\", \"Carlos Roca\", \"croca@uagrm.edu.bo\", \"croca\", \"" + PasswordValidator.EJEMPLO_VALIDO + "\", \"3\", \"fotos/croca.png\", \"true\"]";
                }
                int id = parseEntero(p[0], "id del usuario");
                String nombre = p[1].trim();
                String email = p[2].trim();
                String username = p[3].trim();
                String pass = p[4].trim();
                int idRol = parseEntero(p[5], "idRol (1=Admin, 2=Comisión, 3=Docente)");
                String foto = p.length >= 7 && !p[6].trim().isEmpty() ? p[6].trim() : "fotos/default.png";
                boolean activo = p.length >= 8 ? Boolean.parseBoolean(p[7]) : true;

                // Validación estricta de contraseña en actualización (no se permiten vacíos)
                PasswordValidator.Resultado resPass = PasswordValidator.validar(pass);
                if (!resPass.isValido()) {
                    return resPass.getMensaje();
                }

                String passHash = PasswordHasher.hash(pass);
                Usuario u = new Usuario(id, nombre, email, username, passHash, foto, activo, null, idRol, null);
                boolean ok = usuarioDAO.actualizar(u);
                return ok ? "Usuario ID " + id + " actualizado correctamente." : "Error al actualizar usuario ID " + id + ". Verifique que el ID exista.";
            }

            if (commandUpper.startsWith("DELUSU")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 1) {
                    return "Error de sintaxis en DELUSU: Formato esperado: DELUSU[\"id\"]\nEjemplo: DELUSU[\"2\"]";
                }
                int id = parseEntero(p[0], "id del usuario");
                boolean ok = usuarioDAO.eliminar(id);
                return ok ? "Usuario ID " + id + " eliminado correctamente." : "Error al eliminar usuario ID " + id + ". Verifique que exista y no tenga dependencias.";
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
                    return "Error de sintaxis en INSPLA: Se esperaban 8 parámetros.\n" +
                            "Formato requerido: INSPLA[\"codigo\", \"carrera\", \"anio\", \"descripcion\", \"objetivos\", \"perfil_egreso\", \"minCreditos\", \"maxCreditos\"]\n" +
                            "Ejemplo: INSPLA[\"SIS-2026\", \"Ingenieria en Sistemas\", \"2026\", \"Nuevo plan\", \"Formar profesionales\", \"Perfil de egreso\", \"200\", \"240\"]";
                }
                int anio = parseEntero(p[2], "anio del plan");
                int minCred = parseEntero(p[6], "minCreditos");
                int maxCred = parseEntero(p[7], "maxCreditos");
                PlanEstudio pe = new PlanEstudio(p[0].trim(), p[1].trim(), anio, p[3].trim(), p[4].trim(), p[5].trim(), minCred, maxCred);
                int idGen = planEstudioDAO.insertar(pe);
                return idGen > 0
                        ? "Plan de Estudios registrado exitosamente. ID asignado: " + idGen
                        : "Error al registrar el plan de estudios. Verifique que el código no esté duplicado.";
            }

            if (commandUpper.startsWith("UPDPLA")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 10) {
                    return "Error de sintaxis en UPDPLA: Se esperaban 10 parámetros.\n" +
                            "Formato requerido: UPDPLA[\"id\", \"codigo\", \"carrera\", \"anio\", \"descripcion\", \"objetivos\", \"perfil_egreso\", \"minCred\", \"maxCred\", \"estado\"]\n" +
                            "Ejemplo: UPDPLA[\"1\", \"SIS-2026\", \"Ingenieria en Sistemas\", \"2026\", \"Desc\", \"Obj\", \"Perfil\", \"200\", \"240\", \"En Elaboracion\"]";
                }
                int id = parseEntero(p[0], "id del plan");
                int anio = parseEntero(p[3], "anio del plan");
                int minCred = parseEntero(p[7], "minCred");
                int maxCred = parseEntero(p[8], "maxCred");
                PlanEstudio pe = new PlanEstudio(id, p[1].trim(), p[2].trim(), anio, p[4].trim(), p[5].trim(), p[6].trim(), minCred, maxCred, p[9].trim(), null);
                boolean ok = planEstudioDAO.actualizar(pe);
                return ok ? "Plan de Estudios ID " + id + " actualizado correctamente." : "Error al actualizar plan de estudios ID " + id + ". Verifique que el ID exista.";
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
                    return "Error de sintaxis en INSASIG: Se esperaban 8 parámetros.\n" +
                            "Formato requerido: INSASIG[\"sigla\", \"nombre\", \"semestre\", \"horasTeoricas\", \"horasPracticas\", \"creditos\", \"areaFormativa\", \"idPlanEstudio\"]\n" +
                            "Ejemplo: INSASIG[\"INF110\", \"Introduccion a la Informatica\", \"1\", \"4\", \"2\", \"5\", \"Ciencias de la Computacion\", \"1\"]";
                }
                int sem = parseEntero(p[2], "semestre (1 al 10)");
                int ht = parseEntero(p[3], "horasTeoricas");
                int hp = parseEntero(p[4], "horasPracticas");
                int cred = parseEntero(p[5], "creditos");
                int idPlan = parseEntero(p[7], "idPlanEstudio");

                Asignatura a = new Asignatura(p[0].trim(), p[1].trim(), sem, ht, hp, cred, p[6].trim(), idPlan);
                int idGen = asignaturaDAO.insertar(a);
                return idGen > 0
                        ? "Asignatura registrada exitosamente. ID asignado: " + idGen
                        : "Error al registrar la asignatura. Verifique que la sigla sea única para el plan y que el idPlan exista.";
            }

            if (commandUpper.startsWith("UPDASIG")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 8) {
                    return "Error de sintaxis en UPDASIG: Se esperaban 8 parámetros.\n" +
                            "Formato requerido: UPDASIG[\"id\", \"sigla\", \"nombre\", \"semestre\", \"horasTeoricas\", \"horasPracticas\", \"creditos\", \"areaFormativa\"]\n" +
                            "Ejemplo: UPDASIG[\"1\", \"INF110\", \"Introduccion a la Informatica\", \"1\", \"4\", \"2\", \"5\", \"Ciencias de la Computacion\"]";
                }
                int id = parseEntero(p[0], "id de la asignatura");
                int sem = parseEntero(p[3], "semestre");
                int ht = parseEntero(p[4], "horasTeoricas");
                int hp = parseEntero(p[5], "horasPracticas");
                int cred = parseEntero(p[6], "creditos");

                Asignatura a = new Asignatura(id, p[1].trim(), p[2].trim(), sem, ht, hp, cred, p[7].trim(), 0);
                boolean ok = asignaturaDAO.actualizar(a);
                return ok ? "Asignatura ID " + id + " actualizada correctamente." : "Error al actualizar la asignatura ID " + id + ". Verifique que el ID exista.";
            }

            if (commandUpper.startsWith("DELASIG")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 1) {
                    return "Error de sintaxis en DELASIG: Formato esperado: DELASIG[\"id\"]\nEjemplo: DELASIG[\"1\"]";
                }
                int id = parseEntero(p[0], "id de la asignatura");
                boolean ok = asignaturaDAO.eliminar(id);
                return ok ? "Asignatura ID " + id + " eliminada correctamente." : "Error al eliminar la asignatura ID " + id + ". Verifique si tiene dependencias.";
            }

            // =========================================================================
            // CU3: GESTIÓN DE GRAFO DE PRERREQUISITOS (RF-03, RF-04)
            // =========================================================================
            if (commandUpper.startsWith("INSPRE")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return "Error de sintaxis en INSPRE: Formato esperado: INSPRE[\"idAsignatura\", \"idAsignaturaRequisito\"]\n" +
                            "Ejemplo: INSPRE[\"5\", \"1\"]";
                }
                int idAsig = parseEntero(p[0], "idAsignatura");
                int idReq = parseEntero(p[1], "idAsignaturaRequisito");

                int resultado = prerrequisitoDAO.insertarConValidacion(idAsig, idReq);
                if (resultado == 1) {
                    return "Prerrequisito registrado exitosamente. La asignatura ID " + idAsig + " ahora requiere ID " + idReq + ".";
                } else if (resultado == -1) {
                    return "RECHAZADO POR VALIDACIÓN DE GRAFO (RF-03): La inserción crearía una dependencia circular (ciclo directo o indirecto) entre las materias.";
                } else {
                    return "Error al registrar prerrequisito. Verifique que ambas materias existan y que no esté registrado previamente.";
                }
            }

            if (commandUpper.startsWith("DELPRE")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return "Error de sintaxis en DELPRE: Formato esperado: DELPRE[\"idAsignatura\", \"idAsignaturaRequisito\"]\n" +
                            "Ejemplo: DELPRE[\"5\", \"1\"]";
                }
                int idAsig = parseEntero(p[0], "idAsignatura");
                int idReq = parseEntero(p[1], "idAsignaturaRequisito");
                boolean ok = prerrequisitoDAO.eliminar(idAsig, idReq);
                return ok ? "Prerrequisito eliminado exitosamente." : "Error al eliminar el prerrequisito especificado.";
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
                    return "Error de sintaxis en INSCOM: Formato esperado: INSCOM[\"numero\", \"nombre\", \"descripcion\", \"idPlanEstudio\"]\n" +
                            "Ejemplo: INSCOM[\"1\", \"Marco Referencial\", \"Bases institucionales\", \"1\"]";
                }
                int num = parseEntero(p[0], "numeroComponente");
                int idPlan = parseEntero(p[3], "idPlanEstudio");
                Componente c = new Componente(num, p[1].trim(), p[2].trim(), idPlan);
                int idGen = componenteDAO.insertar(c);
                return idGen > 0
                        ? "Componente registrado exitosamente. ID asignado: " + idGen
                        : "Error al registrar componente. Verifique que el número no esté duplicado en el plan y que el idPlan exista.";
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
                    return "Error de sintaxis en INSCAP: Formato esperado: INSCAP[\"numero\", \"titulo\", \"contenido\", \"idComponente\"]\n" +
                            "Ejemplo: INSCAP[\"1\", \"Justificacion Curricular\", \"Texto explicativo...\", \"1\"]";
                }
                int num = parseEntero(p[0], "numeroCapitulo");
                int idComp = parseEntero(p[3], "idComponente");
                Capitulo cap = new Capitulo(num, p[1].trim(), p[2].trim(), idComp);
                int idGen = capituloDAO.insertar(cap);
                return idGen > 0
                        ? "Capítulo registrado exitosamente en estado 'Borrador'. ID asignado: " + idGen
                        : "Error al registrar capítulo. Verifique que el número no esté duplicado en el componente y que el idComponente exista.";
            }

            if (commandUpper.startsWith("UPDCAP")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 5) {
                    return "Error de sintaxis en UPDCAP: Formato esperado: UPDCAP[\"id\", \"numero\", \"titulo\", \"contenido\", \"idComponente\"]\n" +
                            "Ejemplo: UPDCAP[\"1\", \"1\", \"Justificacion Curricular Actualizada\", \"Nuevo texto...\", \"1\"]";
                }
                int id = parseEntero(p[0], "id del capitulo");
                int num = parseEntero(p[1], "numero de capitulo");
                int idComp = parseEntero(p[4], "idComponente");

                Capitulo cap = new Capitulo(id, num, p[2].trim(), p[3].trim(), idComp, 0, null);
                boolean ok = capituloDAO.actualizar(cap);
                return ok ? "Capítulo ID " + id + " actualizado correctamente." : "Error al actualizar capítulo ID " + id + ". Verifique que el ID exista.";
            }

            if (commandUpper.startsWith("DELCAP")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 1) {
                    return "Error de sintaxis en DELCAP: Formato esperado: DELCAP[\"id\"]\nEjemplo: DELCAP[\"1\"]";
                }
                int id = parseEntero(p[0], "id del capitulo");
                boolean ok = capituloDAO.eliminar(id);
                return ok ? "Capítulo ID " + id + " eliminado correctamente." : "Error al eliminar capítulo ID " + id + ". Verifique si tiene dependencias.";
            }

            if (commandUpper.startsWith("ASIGCAP")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return "Error de sintaxis en ASIGCAP: Formato esperado: ASIGCAP[\"idCapitulo\", \"idUsuario\"]\nEjemplo: ASIGCAP[\"1\", \"3\"]";
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                int idUsu = parseEntero(p[1], "idUsuario");
                boolean ok = capituloDAO.asignarRedactor(idCap, idUsu);
                return ok
                        ? "Redactor ID " + idUsu + " asignado exitosamente al Capítulo ID " + idCap + "."
                        : "Error al asignar redactor. Verifique que ambos identificadores existan.";
            }

            if (commandUpper.startsWith("DELASIGCAP")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return "Error de sintaxis en DELASIGCAP: Formato esperado: DELASIGCAP[\"idCapitulo\", \"idUsuario\"]\nEjemplo: DELASIGCAP[\"1\", \"3\"]";
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                int idUsu = parseEntero(p[1], "idUsuario");
                boolean ok = capituloDAO.removerRedactor(idCap, idUsu);
                return ok
                        ? "Asignación del redactor ID " + idUsu + " removida del Capítulo ID " + idCap + "."
                        : "Error al remover asignación.";
            }

            // =========================================================================
            // CU5: MÁQUINA DE ESTADOS, OBSERVACIONES Y AUDITORÍA (RF-06, RF-07, RF-08, RF-10)
            // =========================================================================
            if (commandUpper.startsWith("CAMBESTADO")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 4) {
                    return "Error de sintaxis en CAMBESTADO: Formato esperado: CAMBESTADO[\"idCapitulo\", \"nuevoEstado\", \"justificacionTecnica\", \"idUsuario\"]\n" +
                            "Ejemplo: CAMBESTADO[\"1\", \"En Revision\", \"Se completo la primera version\", \"3\"]";
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                String nuevoEstado = p[1].trim();
                String justificacion = p[2].trim();
                int idUsu = parseEntero(p[3], "idUsuario");

                Capitulo cap = capituloDAO.obtenerPorId(idCap);
                if (cap == null) {
                    return "Error: No existe un capítulo con ID " + idCap + ".";
                }

                int nuevoEstadoId = resolverIdEstado(nuevoEstado);
                if (nuevoEstadoId == -1) {
                    return "Estado inválido ('" + nuevoEstado + "'). Los estados permitidos son: 'Borrador', 'En Revision', 'Observado', 'Aprobado'.";
                }

                // VALIDACIÓN ESTRICTA PARA APROBACIÓN (RF-07)
                if (nuevoEstadoId == 4) { // 4 = Aprobado
                    int obsPendientes = observacionDAO.contarObservacionesPendientes(idCap);
                    if (obsPendientes > 0) {
                        return "RECHAZO DE APROBACIÓN (RF-07): El Capítulo ID " + idCap + " tiene " + obsPendientes +
                                " observación(es) técnica(s) pendiente(s) de subsanación. Todas las observaciones deben ser resueltas antes de aprobar.";
                    }

                    int evidencias = capituloDAO.contarEvidencias(idCap);
                    if (evidencias == 0) {
                        return "RECHAZO DE APROBACIÓN (RF-07): El Capítulo ID " + idCap +
                                " no cuenta con ninguna evidencia documental obligatoria vinculada. Adjunte resoluciones/respaldos antes de aprobar.";
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
                            idUsu
                    ));
                    return "Transición exitosa: El Capítulo ID " + idCap + " ('" + cap.getTitulo() + "') pasó de '" +
                            estadoAnterior + "' a '" + nuevoEstado + "'. Auditoría registrada.";
                } else {
                    return "Error al actualizar el estado del capítulo en la base de datos.";
                }
            }

            if (commandUpper.startsWith("INSOBS")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 3) {
                    return "Error de sintaxis en INSOBS: Formato esperado: INSOBS[\"idCapitulo\", \"comentarioObservacion\", \"idUsuarioAutoridad\"]\n" +
                            "Ejemplo: INSOBS[\"1\", \"Falta especificar la bibliografia obligatoria\", \"2\"]";
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                String comentario = p[1].trim();
                int idAutoridad = parseEntero(p[2], "idUsuarioAutoridad");

                Capitulo cap = capituloDAO.obtenerPorId(idCap);
                if (cap == null) {
                    return "Error: No existe un capítulo con ID " + idCap + ".";
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
                            idAutoridad
                    ));
                    return "Observación técnica #" + idGen + " registrada exitosamente para el Capítulo ID " + idCap +
                            ". El capítulo pasó automáticamente al estado 'Observado'.";
                } else {
                    return "Error al registrar la observación técnica.";
                }
            }

            if (commandUpper.startsWith("SUBSANOBS")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 3) {
                    return "Error de sintaxis en SUBSANOBS: Formato esperado: SUBSANOBS[\"idObservacion\", \"respuestaSubsanacion\", \"idUsuarioDelegado\"]\n" +
                            "Ejemplo: SUBSANOBS[\"1\", \"Se incorporaron las citas en formato APA septima edicion\", \"3\"]";
                }
                int idObs = parseEntero(p[0], "idObservacion");
                String respuesta = p[1].trim();
                int idDelegado = parseEntero(p[2], "idUsuarioDelegado");

                Observacion obs = observacionDAO.obtenerPorId(idObs);
                if (obs == null) {
                    return "Error: No existe una observación con ID " + idObs + ".";
                }

                boolean ok = observacionDAO.subsanar(idObs, respuesta, idDelegado);
                if (ok) {
                    return "Observación técnica #" + idObs + " subsanada correctamente por el delegado ID " + idDelegado + ".";
                } else {
                    return "Error al subsanar la observación.";
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
            // CU6: REPOSITORIO DE EVIDENCIAS Y BÚSQUEDA FACETADA (RF-11, RF-12, RF-13, RF-14)
            // =========================================================================
            if (commandUpper.startsWith("INSDOC")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 6) {
                    return "Error de sintaxis en INSDOC: Se esperaban al menos 6 parámetros.\n" +
                            "Formato requerido: INSDOC[\"codigoNorma\", \"titulo\", \"tipoDoc\", \"organoEmisor\", \"fechaEmision(AAAA-MM-DD)\", \"archivoRuta\", \"idDocPadre(opcional)\"]\n" +
                            "Ejemplo: INSDOC[\"RES-045/2026\", \"Resolucion de Comision\", \"Resolucion\", \"Consejo Facultativo\", \"2026-03-15\", \"evidencias/res045.pdf\", \"0\"]";
                }
                Date fecha = parseFecha(p[4], "fechaEmision");
                Integer idPadre = (p.length >= 7 && !p[6].trim().isEmpty() && !p[6].trim().equals("0"))
                        ? parseEntero(p[6], "idDocPadre") : null;

                DocumentoEvidencia doc = new DocumentoEvidencia(p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(), fecha, p[5].trim(), idPadre);
                int idGen = documentoDAO.insertar(doc);
                return idGen > 0
                        ? "Documento de evidencia registrado exitosamente. ID asignado: " + idGen
                        : "Error al registrar documento. Verifique que el código de norma sea único.";
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
                    return "Error de sintaxis en VINCULEVI: Formato esperado: VINCULEVI[\"idCapitulo\", \"idDocumentoEvidencia\"]\nEjemplo: VINCULEVI[\"1\", \"5\"]";
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                int idDoc = parseEntero(p[1], "idDocumentoEvidencia");

                boolean ok = documentoDAO.vincularACapitulo(idCap, idDoc);
                return ok
                        ? "Evidencia ID " + idDoc + " vinculada exitosamente como respaldo al Capítulo ID " + idCap + " (Habilitado para validación formal RF-13)."
                        : "Error al vincular evidencia. Verifique que el capítulo y el documento existan.";
            }

            if (commandUpper.startsWith("DELVINCULEVI")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return "Error de sintaxis en DELVINCULEVI: Formato esperado: DELVINCULEVI[\"idCapitulo\", \"idDocumentoEvidencia\"]\nEjemplo: DELVINCULEVI[\"1\", \"5\"]";
                }
                int idCap = parseEntero(p[0], "idCapitulo");
                int idDoc = parseEntero(p[1], "idDocumentoEvidencia");

                boolean ok = documentoDAO.desvincularDeCapitulo(idCap, idDoc);
                return ok
                        ? "Evidencia ID " + idDoc + " desvinculada del Capítulo ID " + idCap + "."
                        : "Error al desvincular evidencia.";
            }

            if (commandUpper.startsWith("BUSDOC")) {
                String[] p = extraerParams(trimmed);
                if (p == null || p.length < 2) {
                    return "Error de sintaxis en BUSDOC: Formato esperado: BUSDOC[\"criterio\", \"valor\"]\n" +
                            "Criterios admitidos: 'TIPO', 'EMISOR', 'CODIGO', 'CAPITULO', 'VIGENTE' o texto libre.\n" +
                            "Ejemplo: BUSDOC[\"TIPO\", \"Resolucion\"]";
                }
                String criterio = p[0].trim();
                String valor = p[1].trim();

                List<DocumentoEvidencia> docs = documentoDAO.busquedaFacetada(criterio, valor);
                if (docs.isEmpty()) {
                    return "No se encontraron documentos de respaldo para el criterio '" + criterio + " = " + valor + "'.";
                }
                return generarTablaDocumentosHTML(docs, "Resultado de Búsqueda Facetada (" + criterio + ": " + valor + ")");
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
                    return "Error de sintaxis en REPORTEFINAL: Formato esperado: REPORTEFINAL[\"idPlanEstudio\"]\nEjemplo: REPORTEFINAL[\"1\"]";
                }
                int idPlan = parseEntero(p[0], "idPlanEstudio");
                ReporteDAO.ReporteFinalConsolidado rep = reporteDAO.compilarReporteFinal(idPlan);
                if (rep == null || rep.plan == null) {
                    return "Error: No existe el Plan de Estudios con ID " + idPlan + ".";
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

            return "Comando '" + subject + "' no implementado aún o no reconocido.\n" +
                    "Por favor envíe un correo con Asunto: HELP[\"*\"] para consultar el manual del sistema.";

        } catch (NumberFormatException e) {
            return "Error en parámetro: " + e.getMessage();
        } catch (IllegalArgumentException e) {
            return "Error en parámetro de fecha u opción: " + e.getMessage();
        } catch (Exception e) {
            return "Error al procesar el comando: " + e.getMessage();
        }
    }

    private int parseEntero(String valor, String nombreCampo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new NumberFormatException("El campo '" + nombreCampo + "' es obligatorio y no puede estar vacío.");
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("El campo '" + nombreCampo + "' debe ser un número entero válido (valor recibido: '" + valor.trim() + "').");
        }
    }

    private Date parseFecha(String valor, String nombreCampo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo '" + nombreCampo + "' es obligatorio y no puede estar vacío. Formato requerido: AAAA-MM-DD.");
        }
        try {
            return Date.valueOf(valor.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El campo '" + nombreCampo + "' debe tener el formato de fecha AAAA-MM-DD (ejemplo: 2026-10-15). Valor recibido: '" + valor.trim() + "'.");
        }
    }

    private String verificarPermisos(String comando, String remitente) {
        // Comandos públicos que no requieren sesión/rol previo:
        if (comando.startsWith("HELP") || comando.startsWith("LOGIN") || comando.startsWith("LISPLA") || comando.startsWith("LISASIG") || comando.startsWith("VERGRAFO")) {
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
        // Rol 1 = Administrador (acceso total a todas las operaciones)
        if (rol == 1) {
            return null;
        }

        // Restricciones para Rol 2 (Autoridad / Comisión Curricular)
        if (rol == 2) {
            if (comando.startsWith("INSUSU") || comando.startsWith("UPDUSU") || comando.startsWith("DELUSU")) {
                return "Permiso denegado: Su rol 'Comisión Curricular' no tiene permisos para gestionar usuarios (INSUSU/UPDUSU/DELUSU). Acción reservada para Administradores.";
            }
            if (comando.startsWith("INSPLA") || comando.startsWith("UPDPLA")) {
                return "Permiso denegado: Su rol 'Comisión Curricular' no tiene permisos para crear o modificar planes de estudio. Acción reservada para Administradores.";
            }
            return null; // Habilitado para componentes, capítulos, observaciones, evidencias, tableros y reporte final
        }

        // Restricciones para Rol 3 (Docente Redactor)
        if (rol == 3) {
            if (comando.startsWith("INSUSU") || comando.startsWith("UPDUSU") || comando.startsWith("DELUSU") ||
                    comando.startsWith("INSPLA") || comando.startsWith("UPDPLA") ||
                    comando.startsWith("INSASIG") || comando.startsWith("UPDASIG") || comando.startsWith("DELASIG") ||
                    comando.startsWith("INSPRE") || comando.startsWith("DELPRE") ||
                    comando.startsWith("INSCOM") || comando.startsWith("ASIGCAP") || comando.startsWith("DELASIGCAP") ||
                    comando.startsWith("CAMBESTADO") || comando.startsWith("INSOBS")) {
                return "Permiso denegado: Su rol 'Docente Redactor' no tiene autorización para ejecutar el comando '" + comando + "'.\n" +
                        "Sus permisos están limitados a redactar capítulos (INSCAP/UPDCAP), subsanar observaciones (SUBSANOBS), vincular evidencias (VINCULEVI) y consultar su avance (REPAVANCE).";
            }
            return null;
        }

        return null;
    }

    private String generarAlertaAccesoDenegadoHTML(String remitente, String comando) {
        return "<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px; line-height: 1.5;'>" +
                "<div style='background-color: #c0392b; color: white; padding: 15px 20px; border-radius: 6px; max-width: 650px;'>" +
                "<h2 style='margin: 0; font-size: 20px;'>🚫 Acceso Restringido - Autenticación Requerida</h2>" +
                "</div>" +
                "<div style='background-color: #fdf7f7; border: 1px solid #ebccd1; padding: 18px; border-radius: 6px; margin-top: 15px; max-width: 650px;'>" +
                "<p style='margin: 0 0 10px 0; font-size: 14px; color: #a94442;'>" +
                "La dirección de correo remitente <b>&lt;" + remitente + "&gt;</b> no se encuentra vinculada a ningún usuario activo en el sistema." +
                "</p>" +
                "<p style='margin: 0; font-size: 13px; color: #555;'>" +
                "<b>Comando solicitado:</b> <code style='background: #eee; padding: 2px 6px; border-radius: 3px; color: #c0392b;'>" + comando + "</code>" +
                "</p>" +
                "</div>" +
                "<div style='background-color: #f7f9fc; border: 1px solid #e1e8ed; padding: 18px; border-radius: 6px; margin-top: 15px; max-width: 650px;'>" +
                "<h4 style='margin: 0 0 10px 0; color: #003366;'>¿Cómo vincular su correo y autorizar sus operaciones?</h4>" +
                "<p style='font-size: 13px; margin: 0 0 10px 0; color: #444;'>" +
                "Para iniciar sesión y asociar este buzón de correo a su cuenta institucional, envíe en el <b>Asunto</b> de un nuevo correo:" +
                "</p>" +
                "<div style='background-color: #eef2f7; border-left: 4px solid #003366; padding: 12px; font-family: monospace; font-size: 13px; color: #003366; margin-bottom: 10px;'>" +
                "LOGIN[\"su_correo@institucional.edu.bo\", \"su_contraseña\"]" +
                "</div>" +
                "<p style='font-size: 12px; color: #666; margin: 0;'>" +
                "• <i>Ejemplo Administrador Inicial:</i> <code>LOGIN[\"grupo02sa@tecnoweb.org.bo\", \"admin123\"]</code>" +
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
                "<p style='font-size: 11px; color: #888; margin-top: 15px;'>Sistema de Seguimiento al Diseño Curricular - UAGRM TecnoWeb 2026</p>" +
                "</body></html>";
    }

    private String generarPerfilUsuarioHTML(Usuario u) {
        String rolColor = u.getIdRol() == 1 ? "#c0392b" : (u.getIdRol() == 2 ? "#2980b9" : "#27ae60");
        String rolBadge = "<span style='background-color: " + rolColor + "; color: white; padding: 4px 10px; border-radius: 4px; font-weight: bold;'>" + (u.getNombreRol() != null ? u.getNombreRol() : "Rol " + u.getIdRol()) + "</span>";

        String permisosTexto = (u.getIdRol() == 1)
                ? "• <b>Acceso Total (Administrador):</b> LISUSU, INSUSU, UPDUSU, DELUSU, LISPLA, INSPLA, UPDPLA, LISASIG, INSASIG, UPDASIG, DELASIG, INSPRE, DELPRE, LISPRE, VERGRAFO, LISCOM, INSCOM, LISCAP, INSCAP, UPDCAP, DELCAP, ASIGCAP, DELASIGCAP, CAMBESTADO, INSOBS, LISOBS, SUBSANOBS, LISHIST, INSDOC, LISDOC, VINCULEVI, DELVINCULEVI, BUSDOC, VERDASH, REPORTEFINAL, REPAVANCE, REPUSUARIOS, HELP."
                : ((u.getIdRol() == 2)
                ? "• <b>Comisión Curricular / Autoridad:</b> Revisión y transición de capítulos (CAMBESTADO), emisión de observaciones (INSOBS), componentes y evidencias (INSCOM, INSDOC, VINCULEVI), tablero de control (VERDASH) y compilador oficial (REPORTEFINAL)."
                : "• <b>Docente Redactor:</b> Redacción de capítulos asignados (INSCAP, UPDCAP), subsanación de observaciones técnicas (SUBSANOBS), vinculación de evidencias (VINCULEVI) y monitoreo de avance personal (REPAVANCE).");

        return "<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>" +
                "<div style='background-color: #003366; color: white; padding: 15px; border-radius: 6px;'>" +
                "<h2 style='margin: 0;'>Inicio de Sesión Exitoso</h2>" +
                "<p style='margin: 4px 0 0 0; font-size: 13px;'>Sistema de Seguimiento al Diseño Curricular - TecnoWeb UAGRM</p>" +
                "</div>" +
                "<div style='background-color: #f7f9fc; border: 1px solid #e1e8ed; padding: 20px; border-radius: 6px; margin-top: 15px; max-width: 650px;'>" +
                "<table cellpadding='8' style='width: 100%; border-collapse: collapse; font-size: 14px;'>" +
                "<tr><td style='width: 150px;'><b>Nombre Completo:</b></td><td>" + u.getNombreCompleto() + "</td></tr>" +
                "<tr><td><b>Nombre de Usuario:</b></td><td><code>" + u.getUsername() + "</code></td></tr>" +
                "<tr><td><b>Correo Vinculado:</b></td><td>" + u.getEmail() + "</td></tr>" +
                "<tr><td><b>Rol Asignado:</b></td><td>" + rolBadge + "</td></tr>" +
                "<tr><td><b>Ruta de Foto:</b></td><td style='font-size: 12px; color: #666;'>" + u.getFotoRuta() + "</td></tr>" +
                "</table>" +
                "<hr style='border: 0; border-top: 1px solid #ddd; margin: 15px 0;'/>" +
                "<h4 style='margin: 0 0 8px 0; color: #003366;'>Comandos Habilitados para su Rol:</h4>" +
                "<p style='margin: 0; font-size: 13px; line-height: 1.6; color: #444;'>" + permisosTexto + "</p>" +
                "</div>" +
                "<p style='margin-top: 15px; font-size: 12px; color: #888;'>Su correo actual ha quedado vinculado en la base de datos para autorizar sus comandos por correo.</p>" +
                "</body></html>";
    }

    private int resolverIdEstado(String estado) {
        String clean = estado.trim().toLowerCase();
        if (clean.contains("borrador")) return 1;
        if (clean.contains("revisión") || clean.contains("revision")) return 2;
        if (clean.contains("observado")) return 3;
        if (clean.contains("aprobado")) return 4;
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
        sb.append("<div style='background-color: #f7f9fc; border: 1px solid #e1e8ed; padding: 15px; border-radius: 6px; margin: 15px 0;'>")
                .append("<div style='display: flex; justify-content: space-between; align-items: center;'>")
                .append("<h3 style='margin: 0; color: #003366;'>Avance Global del Documento Oficial: <span style='color: ").append(colorBarra).append(";'>")
                .append(String.format("%.1f", m.porcentajeAvance)).append("%</span></h3>")
                .append("</div>")
                .append("<div style='background-color: #e0e0e0; border-radius: 8px; height: 16px; width: 100%; margin-top: 10px; overflow: hidden;'>")
                .append("<div style='background-color: ").append(colorBarra).append("; width: ").append((int) m.porcentajeAvance).append("%; height: 100%;'></div>")
                .append("</div>")
                .append("<p style='margin: 6px 0 0 0; font-size: 12px; color: #666;'>").append(m.aprobadoCount).append(" de ").append(m.totalCapitulos).append(" capítulos aprobados formalmente</p>")
                .append("</div>");

        // Tarjetas métricas de estado
        sb.append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; text-align: center; margin-bottom: 20px; border-color: #ddd;'>")
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
            sb.append("<div style='background-color: #fceae8; border-left: 5px solid #c0392b; padding: 12px; margin-bottom: 20px;'>")
                    .append("<h4 style='margin: 0; color: #c0392b;'>ALERTA: Secciones Observadas Pendientes de Subsanación (").append(m.capitulosObservados.size()).append(")</h4>")
                    .append("<p style='margin: 5px 0 10px 0; font-size: 12px;'>Los siguientes capítulos no podrán ser aprobados hasta que la comisión responda y cierre los señalamientos:</p>")
                    .append("<table border='1' cellpadding='6' style='border-collapse: collapse; width: 100%; font-size: 12px; border-color: #ddd;'>")
                    .append("<tr style='background-color: #f9d5d1; color: #900;'><th>ID</th><th>Capítulo</th><th>Componente</th><th>Acción</th></tr>");

            for (Capitulo c : m.capitulosObservados) {
                sb.append("<tr>")
                        .append("<td align='center'>").append(c.getId()).append("</td>")
                        .append("<td><b>Capítulo ").append(c.getNumero()).append(": ").append(c.getTitulo()).append("</b></td>")
                        .append("<td>").append(c.getNombreComponente()).append("</td>")
                        .append("<td align='center'><code>LISOBS[\"").append(c.getId()).append("\"]</code></td>")
                        .append("</tr>");
            }
            sb.append("</table></div>");
        } else {
            sb.append("<div style='background-color: #eafaf1; border-left: 5px solid #27ae60; padding: 10px; margin-bottom: 20px;'>")
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
                .append("<p style='margin: 0; font-size: 14px;'>Carrera: <b>").append(rep.plan.getCarrera()).append("</b> | Plan: <b>").append(rep.plan.getCodigo())
                .append("</b> | Año: <b>").append(rep.plan.getAnioVigencia()).append("</b> | Estado: <b>").append(rep.plan.getEstado()).append("</b></p>")
                .append("</div>");

        // 1. Marco Filosófico y Perfil de Egreso
        sb.append("<h2 style='color: #003366; border-bottom: 1px solid #ddd; padding-bottom: 5px;'>1. Objetivos del Plan y Perfil de Egreso</h2>")
                .append("<h4>Objetivos de la Carrera:</h4>")
                .append("<p style='background-color: #f7f9fc; padding: 10px; border-left: 4px solid #003366;'>").append(rep.plan.getObjetivos()).append("</p>")
                .append("<h4>Perfil de Egreso:</h4>")
                .append("<p style='background-color: #f7f9fc; padding: 10px; border-left: 4px solid #003366;'>").append(rep.plan.getPerfilEgreso()).append("</p>")
                .append("<p><b>Límites de Créditos Formativos:</b> Mínimo ").append(rep.plan.getLimiteCreditosMin()).append(" - Máximo ").append(rep.plan.getLimiteCreditosMax()).append(" créditos.</p>");

        // 2. Malla Curricular Consolidada
        sb.append("<h2 style='color: #003366; border-bottom: 1px solid #ddd; padding-bottom: 5px; margin-top: 30px;'>2. Malla Curricular Oficial</h2>")
                .append("<p>Total Asignaturas: <b>").append(rep.asignaturas.size()).append("</b> | Total Créditos Consolidados: <b>").append(rep.totalCreditos).append(" créditos</b></p>")
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
        sb.append("<h2 style='color: #003366; border-bottom: 1px solid #ddd; padding-bottom: 5px; margin-top: 30px;'>3. Capítulos Oficiales Aprobados</h2>");
        if (rep.capitulosAprobados.isEmpty()) {
            sb.append("<p style='color: #c0392b; font-style: italic;'>Aún no existen capítulos en estado 'Aprobado' para compilar formalmente.</p>");
        } else {
            for (Capitulo c : rep.capitulosAprobados) {
                sb.append("<div style='margin-bottom: 25px;'>")
                        .append("<h3 style='color: #2c3e50; margin-bottom: 4px;'>Capítulo ").append(c.getNumero()).append(": ").append(c.getTitulo()).append("</h3>")
                        .append("<p style='font-size: 12px; color: #7f8c8d; margin-top: 0;'>Componente: <b>").append(c.getNombreComponente()).append("</b> | Redactores: <b>").append(c.getResponsables()).append("</b> | Estado: <b style='color: green;'>Aprobado</b></p>")
                        .append("<div style='background-color: #fafafa; border: 1px solid #eee; padding: 12px; border-radius: 4px;'>")
                        .append("<p style='margin: 0;'>").append(c.getContenido() != null ? c.getContenido() : "<i>Texto en custodia institucional</i>").append("</p>")
                        .append("</div></div>");
            }
        }

        // 4. Anexos Normativos Vigentes
        sb.append("<h2 style='color: #003366; border-bottom: 1px solid #ddd; padding-bottom: 5px; margin-top: 30px;'>4. Anexos de Resoluciones y Evidencias Normativas Vigentes</h2>")
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

        sb.append("<p style='margin-top: 30px; font-size: 11px; color: #999; text-align: center;'>Documento generado automáticamente por el Sistema de Seguimiento Curricular - TecnoWeb UAGRM</p>")
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
                    .append("<td align='center'><b style='color: ").append(color).append(";'>").append(String.format("%.1f", c.porcentaje)).append("%</b></td>")
                    .append("<td><div style='background-color: #eee; width: 100%; height: 12px; border-radius: 4px; overflow: hidden;'>")
                    .append("<div style='background-color: ").append(color).append("; width: ").append((int) c.porcentaje).append("%; height: 100%;'></div>")
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
                    .append("<td align='center'><b style='color: green;'>").append(u.observacionesSubsanadas).append("</b></td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarTablaUsuariosHTML(List<Usuario> usuarios) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<h2 style='color: #003366;'>Listado de Usuarios Registrados</h2>")
                .append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd;'>")
                .append("<tr style='background-color: #003366; color: white;'>")
                .append("<th>ID</th><th>Nombre Completo</th><th>Email</th><th>Usuario</th><th>Rol</th><th>Foto</th><th>Estado</th>")
                .append("</tr>");

        for (Usuario u : usuarios) {
            sb.append("<tr>")
                    .append("<td align='center'>").append(u.getId()).append("</td>")
                    .append("<td><b>").append(u.getNombreCompleto()).append("</b></td>")
                    .append("<td>").append(u.getEmail()).append("</td>")
                    .append("<td>").append(u.getUsername()).append("</td>")
                    .append("<td align='center'>").append(u.getNombreRol() != null ? u.getNombreRol() : u.getIdRol()).append("</td>")
                    .append("<td style='font-size: 11px; color: #666;'>").append(u.getFotoRuta() != null ? u.getFotoRuta() : "-").append("</td>")
                    .append("<td align='center'>").append(u.isActivo() ? "<span style='color: green;'>Activo</span>" : "<span style='color: red;'>Inactivo</span>").append("</td>")
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
                    .append("<td align='center'>").append(p.getLimiteCreditosMin()).append(" - ").append(p.getLimiteCreditosMax()).append("</td>")
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
                .append("<p style='font-size: 14px;'>Asignatura Evaluada: <b>").append(materiaPrincipal).append("</b> (ID: ").append(idAsig).append(")</p>")
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

    private String generarVistaGrafoHTML(Asignatura actual, List<Asignatura> haciaAtras, List<Asignatura> haciaAdelante) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<div style='background-color: #003366; color: white; padding: 12px; border-radius: 6px;'>")
                .append("<h2 style='margin: 0;'>Grafo Curricular de Prelación (RF-04)</h2>")
                .append("<p style='margin: 4px 0 0 0;'>Trazabilidad hacia atrás (requisitos previos) y hacia adelante (materias dependientes)</p>")
                .append("</div>");

        sb.append("<div style='background-color: #eef4fb; border-left: 5px solid #003366; padding: 12px; margin: 15px 0;'>")
                .append("<h3 style='margin: 0; color: #003366;'>Materia Central: [").append(actual.getSigla()).append("] ").append(actual.getNombre()).append("</h3>")
                .append("<p style='margin: 5px 0 0 0; font-size: 13px;'>Semestre: <b>").append(actual.getSemestre())
                .append("</b> | Créditos: <b>").append(actual.getCreditos())
                .append("</b> | Área: <b>").append(actual.getAreaFormativa())
                .append("</b> | ID: <b>").append(actual.getId()).append("</b></p>")
                .append("</div>");

        sb.append("<h4 style='color: #c0392b; margin-bottom: 8px;'>← 1. Cadena de Prerrequisitos Hacia Atrás (Materias que debe cursar antes):</h4>");
        if (haciaAtras.isEmpty()) {
            sb.append("<p style='font-size: 13px; color: #7f8c8d; font-style: italic;'>Esta materia no tiene ningún prerrequisito previo en la malla (Materia de base o Semestre 1).</p>");
        } else {
            sb.append("<table border='1' cellpadding='6' style='border-collapse: collapse; width: 100%; font-size: 12px; border-color: #ddd; margin-bottom: 20px;'>")
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

        sb.append("<h4 style='color: #27ae60; margin-bottom: 8px;'>→ 2. Cadena de Asignaturas Dependientes Hacia Adelante (Materias que desbloquea):</h4>");
        if (haciaAdelante.isEmpty()) {
            sb.append("<p style='font-size: 13px; color: #7f8c8d; font-style: italic;'>Ninguna otra materia de la malla tiene a esta asignatura como prerrequisito (Materia terminal o de último semestre).</p>");
        } else {
            sb.append("<table border='1' cellpadding='6' style='border-collapse: collapse; width: 100%; font-size: 12px; border-color: #ddd;'>")
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
            if ("Aprobado".equalsIgnoreCase(c.getNombreEstado())) colorEstado = "#27ae60";
            else if ("En Revisión".equalsIgnoreCase(c.getNombreEstado())) colorEstado = "#2980b9";
            else if ("Observado".equalsIgnoreCase(c.getNombreEstado())) colorEstado = "#c0392b";

            sb.append("<tr>")
                    .append("<td align='center'>").append(c.getId()).append("</td>")
                    .append("<td align='center'><b>").append(c.getNumero()).append("</b></td>")
                    .append("<td><b>").append(c.getTitulo()).append("</b></td>")
                    .append("<td>").append(c.getNombreComponente() != null ? c.getNombreComponente() : c.getIdComponente()).append("</td>")
                    .append("<td align='center'><b style='color: ").append(colorEstado).append(";'>").append(c.getNombreEstado()).append("</b></td>")
                    .append("<td>").append(c.getResponsables()).append("</td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarTablaObservacionesHTML(int idCap, List<Observacion> lista) {
        StringBuilder sb = new StringBuilder();
        String titulo = lista.get(0).getTituloCapitulo() != null ? lista.get(0).getTituloCapitulo() : "Capítulo #" + idCap;

        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<h2 style='color: #003366;'>Observaciones Técnicas del Capítulo</h2>")
                .append("<p style='font-size: 14px;'>Capítulo Evaluado: <b>").append(titulo).append("</b> (ID: ").append(idCap).append(")</p>")
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
                    .append("<td><b>").append(o.getNombreObservador() != null ? o.getNombreObservador() : o.getIdUsuarioObservador()).append("</b></td>")
                    .append("<td>").append(o.getComentario()).append("</td>")
                    .append("<td align='center'>").append(badge).append("</td>")
                    .append("<td>").append(o.getNombreRespuesta() != null ? o.getNombreRespuesta() : "-").append("</td>")
                    .append("<td>").append(o.getRespuestaDelegado() != null ? o.getRespuestaDelegado() : "<i style='color: #888;'>Sin respuesta</i>").append("</td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarTablaHistorialHTML(int idCap, List<HistorialModificacion> lista) {
        StringBuilder sb = new StringBuilder();
        String titulo = lista.get(0).getTituloCapitulo() != null ? lista.get(0).getTituloCapitulo() : "Capítulo #" + idCap;

        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<div style='background-color: #003366; color: white; padding: 12px; border-radius: 6px;'>")
                .append("<h2 style='margin: 0;'>Pista de Auditoría Histórica (RF-10)</h2>")
                .append("<p style='margin: 4px 0 0 0;'>Registro inmutable de cambios de estado para: <b>").append(titulo).append("</b> (ID: ").append(idCap).append(")</p>")
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
                    .append("<td align='center'><b style='color: #2980b9;'>").append(h.getEstadoNuevo()).append("</b></td>")
                    .append("<td>").append(h.getJustificacionTecnica()).append("</td>")
                    .append("<td><b>").append(h.getNombreUsuario() != null ? h.getNombreUsuario() : h.getIdUsuario()).append("</b></td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarTablaDocumentosHTML(List<DocumentoEvidencia> docs, String tituloSeccion) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333; margin: 20px;'>")
                .append("<div style='background-color: #003366; color: white; padding: 12px; border-radius: 6px;'>")
                .append("<h2 style='margin: 0;'>").append(tituloSeccion).append("</h2>")
                .append("<p style='margin: 4px 0 0 0;'>Repositorio Digital de Evidencias Normativas (RF-11 a RF-14)</p>")
                .append("</div>")
                .append("<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd; margin-top: 15px;'>")
                .append("<tr style='background-color: #f2f5f9; color: #003366;'>")
                .append("<th>ID</th><th>Código Norma</th><th>Título / Descripción</th><th>Tipo</th><th>Órgano Emisor</th><th>Emisión</th><th>Archivo / Enlace</th><th>Vigencia</th><th>Norma Padre</th>")
                .append("</tr>");

        for (DocumentoEvidencia d : docs) {
            String vigenciaBadge = d.isVigente()
                    ? "<span style='color: #27ae60; font-weight: bold;'>Vigente</span>"
                    : "<span style='color: #c0392b; font-weight: bold;'>Derogada/Obs</span>";

            String padreInfo = d.getCodigoNormaPadre() != null ? d.getCodigoNormaPadre() : "-";

            sb.append("<tr>")
                    .append("<td align='center'>").append(d.getId()).append("</td>")
                    .append("<td><b>").append(d.getCodigoNorma()).append("</b></td>")
                    .append("<td>").append(d.getTitulo()).append("</td>")
                    .append("<td align='center'>").append(d.getTipoDocumento()).append("</td>")
                    .append("<td>").append(d.getOrganoEmisor()).append("</td>")
                    .append("<td align='center'>").append(d.getFechaEmision()).append("</td>")
                    .append("<td style='font-size: 11px; color: #2980b9;'>").append(d.getArchivoRuta()).append("</td>")
                    .append("<td align='center'>").append(vigenciaBadge).append("</td>")
                    .append("<td align='center'>").append(padreInfo).append("</td>")
                    .append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String generarManualHTML() {
        return "<html><body style='font-family: Arial, sans-serif; color: #333; line-height: 1.5; margin: 20px;'>" +
                "<div style='background-color: #003366; color: white; padding: 15px; border-radius: 6px;'>" +
                "<h2 style='margin: 0;'>Sistema de Seguimiento al Diseño Curricular</h2>" +
                "<p style='margin: 5px 0 0 0; font-size: 14px;'>Grupo 02 SA - Universidad Autónoma Gabriel René Moreno</p>" +
                "</div>" +
                "<h3 style='color: #003366; margin-top: 25px;'>Manual Completo de Comandos (CU1 al CU8)</h3>" +
                "<table border='1' cellpadding='8' style='border-collapse: collapse; width: 100%; font-size: 13px; border-color: #ddd;'>" +
                "<tr style='background-color: #f2f5f9; color: #003366;'>" +
                "<th>Caso de Uso</th><th>Comando de Consulta</th><th>Comando de Inserción / Acción</th>" +
                "</tr>" +
                "<tr><td><b>Acceso / Sesión</b></td><td><code>LOGIN[\"email\",\"pass\"]</code></td><td>Inicia sesión y vincula el correo para autorizar comandos por rol.</td></tr>" +
                "<tr><td><b>CU1. Usuarios</b></td><td><code>LISUSU[\"*\"]</code></td><td><code>INSUSU[\"nom\",\"email\",\"user\",\"pass\",\"idRol\",\"foto\"]</code><br/><code>UPDUSU[...]</code> / <code>DELUSU[\"id\"]</code></td></tr>" +
                "<tr><td><b>CU2. Plan y Materias</b></td><td><code>LISPLA[\"*\"]</code><br/><code>LISASIG[\"idPlan\"]</code></td><td><code>INSPLA[\"cod\",\"carrera\",\"año\",\"desc\",\"obj\",\"perfil\",\"minCred\",\"maxCred\"]</code><br/><code>INSASIG[\"sigla\",\"nom\",\"sem\",\"ht\",\"hp\",\"cred\",\"area\",\"idPlan\"]</code></td></tr>" +
                "<tr><td><b>CU3. Prerrequisitos</b></td><td><code>LISPRE[\"idAsig\"]</code><br/><code>VERGRAFO[\"idAsig\"]</code></td><td><code>INSPRE[\"idAsig\",\"idAsigRequisito\"]</code><br/><code>DELPRE[\"idAsig\",\"idAsigRequisito\"]</code></td></tr>" +
                "<tr><td><b>CU4. Rediseño y Capítulos</b></td><td><code>LISCOM[\"idPlan\"]</code><br/><code>LISCAP[\"idComp\"]</code></td><td><code>INSCOM[\"num\",\"nom\",\"desc\",\"idPlan\"]</code><br/><code>INSCAP[\"num\",\"titulo\",\"contenido\",\"idComp\"]</code><br/><code>ASIGCAP[\"idCap\",\"idUsu\"]</code></td></tr>" +
                "<tr><td><b>CU5. Estados y Auditoría</b></td><td><code>LISOBS[\"idCap\"]</code><br/><code>LISHIST[\"idCap\"]</code></td><td><code>CAMBESTADO[\"idCap\",\"nuevoEstado\",\"justificacion\",\"idUsu\"]</code><br/><code>INSOBS[\"idCap\",\"comentario\",\"idAutoridad\"]</code><br/><code>SUBSANOBS[\"idObs\",\"respuesta\",\"idDelegado\"]</code></td></tr>" +
                "<tr><td><b>CU6. Evidencias Normativas</b></td><td><code>LISDOC[\"*\"]</code><br/><code>BUSDOC[\"criterio\",\"valor\"]</code></td><td><code>INSDOC[\"codNorma\",\"titulo\",\"tipo\",\"emisor\",\"fecha\",\"archivo\",\"idPadre\"]</code><br/><code>VINCULEVI[\"idCap\",\"idDoc\"]</code></td></tr>" +
                "<tr><td><b>CU7. Monitoreo y Compilador</b></td><td><code>VERDASH[\"*\"]</code></td><td><code>REPORTEFINAL[\"idPlan\"]</code> (Compilador consolidado oficial)</td></tr>" +
                "<tr><td><b>CU8. Reportes y Estadísticas</b></td><td><code>REPAVANCE[\"*\"]</code><br/><code>REPUSUARIOS[\"*\"]</code></td><td><code>HELP[\"*\"]</code> (Manual de comandos)</td></tr>" +
                "</table>" +
                "<p style='margin-top: 20px; font-size: 12px; color: #666;'>* Envíe el comando exacto en el campo <b>Asunto (Subject)</b> de su correo.</p>" +
                "</body></html>";
    }
}
