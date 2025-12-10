<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

$payload = requireAuth();
$conn = getDBConnection();

$data = json_decode(file_get_contents('php://input'), true);

// Aceptar múltiples nombres de parámetros
$idDocente = isset($data['id_docente']) ? (int)$data['id_docente'] : (isset($data['docente_id']) ? (int)$data['docente_id'] : null);
$idEstudiante = isset($data['id_estudiante']) ? (int)$data['id_estudiante'] : (isset($data['estudiante_id']) ? (int)$data['estudiante_id'] : null);
$accion = isset($data['accion']) ? $conn->real_escape_string($data['accion']) : null;

// Si no se especifica docente, asumir el usuario autenticado cuando sea docente
if ($idDocente === null && $payload['rol'] === 'docente') {
    $idDocente = $payload['id_usuario'];
}

// Si no se especifica estudiante, mantener compatibilidad para flujos anteriores
if ($idEstudiante === null && $payload['rol'] === 'estudiante') {
    $idEstudiante = $payload['id_usuario'];
}

if ($idDocente === null || $idEstudiante === null || $accion === null) {
    http_response_code(400);
    echo jsonResponse(false, 'id_docente, id_estudiante y accion son requeridos');
    $conn->close();
    exit();
}

// Validar acción
if (!in_array($accion, ['aceptar', 'rechazar'])) {
    http_response_code(400);
    echo jsonResponse(false, 'La acción debe ser "aceptar" o "rechazar"');
    $conn->close();
    exit();
}

$estado = $accion === 'aceptar' ? 'aceptado' : 'rechazado';

// Verificar que existe la relación
$stmt = $conn->prepare("SELECT estado FROM docenteestudiante WHERE id_docente = ? AND id_estudiante = ?");
$stmt->bind_param("ii", $idDocente, $idEstudiante);
$stmt->execute();
$result = $stmt->get_result();

$solicitud = $result->fetch_assoc();

if (!$solicitud) {
    http_response_code(404);
    echo jsonResponse(false, 'No se encontró la solicitud');
    $stmt->close();
    $conn->close();
    exit();
}
$stmt->close();

// Verificar permisos: ahora solo el docente asignado (o admin) puede responder
if (!in_array($payload['rol'], ['docente', 'administrador'])) {
    http_response_code(403);
    echo jsonResponse(false, 'Solo el docente puede responder la solicitud');
    $conn->close();
    exit();
}

if ($payload['rol'] === 'docente' && $idDocente != $payload['id_usuario']) {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para responder esta solicitud');
    $conn->close();
    exit();
}

if ($solicitud['estado'] !== 'pendiente' && $payload['rol'] !== 'administrador') {
    http_response_code(409);
    echo jsonResponse(false, 'Esta solicitud ya fue respondida');
    $conn->close();
    exit();
}

// Actualizar el estado
$stmt = $conn->prepare("UPDATE docenteestudiante SET estado = ? WHERE id_docente = ? AND id_estudiante = ?");
$stmt->bind_param("sii", $estado, $idDocente, $idEstudiante);

if (!$stmt->execute()) {
    http_response_code(500);
    echo jsonResponse(false, 'Error al responder solicitud');
    $stmt->close();
    $conn->close();
    exit();
}

$response = [
    'success' => true,
    'message' => 'Solicitud respondida exitosamente',
    'data' => null
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();
?>




