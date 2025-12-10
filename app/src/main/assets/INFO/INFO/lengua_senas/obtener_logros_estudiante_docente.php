<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

$payload = requireAuth();
$conn = getDBConnection();

ensureDefaultLogros($conn);

// Aceptar múltiples nombres de parámetros
$idDocente = isset($_GET['id_docente']) ? (int)$_GET['id_docente'] : (isset($_GET['docente_id']) ? (int)$_GET['docente_id'] : (isset($_GET['id_usuario']) ? (int)$_GET['id_usuario'] : (isset($_GET['usuario_id']) ? (int)$_GET['usuario_id'] : $payload['id_usuario'])));
$idEstudiante = isset($_GET['id_estudiante']) ? (int)$_GET['id_estudiante'] : (isset($_GET['estudiante_id']) ? (int)$_GET['estudiante_id'] : null);

if ($idEstudiante === null) {
    http_response_code(400);
    echo jsonResponse(false, 'id_estudiante es requerido');
    $conn->close();
    exit();
}

// Verificar permisos: solo el docente puede ver los logros de sus estudiantes
if ($idDocente != $payload['id_usuario'] && $payload['rol'] !== 'administrador') {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para ver estos logros');
    $conn->close();
    exit();
}

// Verificar que existe la relación docente-estudiante
$stmt = $conn->prepare("SELECT estado FROM docenteestudiante WHERE id_docente = ? AND id_estudiante = ? AND estado = 'aceptado'");
$stmt->bind_param("ii", $idDocente, $idEstudiante);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0 && $payload['rol'] !== 'administrador') {
    http_response_code(403);
    echo jsonResponse(false, 'No existe una relación activa con este estudiante');
    $stmt->close();
    $conn->close();
    exit();
}
$stmt->close();

// Obtener información del estudiante
$stmt = $conn->prepare("SELECT id_usuario, nombre, correo, rol FROM usuarios WHERE id_usuario = ?");
$stmt->bind_param("i", $idEstudiante);
$stmt->execute();
$result = $stmt->get_result();
$estudiante = $result->fetch_assoc();
$stmt->close();

if (!$estudiante) {
    http_response_code(404);
    echo jsonResponse(false, 'Estudiante no encontrado');
    $conn->close();
    exit();
}

// Obtener logros almacenados para el estudiante
$stmt = $conn->prepare("
    SELECT l.id_logro, l.titulo, l.descripcion, ul.fecha_obtenido
    FROM usuario_logros ul
    JOIN logros l ON l.id_logro = ul.id_logro
    WHERE ul.id_usuario = ?
    ORDER BY ul.fecha_obtenido DESC
");
$stmt->bind_param("i", $idEstudiante);
$stmt->execute();
$result = $stmt->get_result();

$logros = [];
while ($row = $result->fetch_assoc()) {
    $logros[] = [
        'id_logro' => (int)$row['id_logro'],
        'titulo' => $row['titulo'],
        'descripcion' => $row['descripcion'],
        'fecha_obtenido' => $row['fecha_obtenido']
    ];
}
$stmt->close();

$response = [
    'success' => true,
    'message' => 'Logros obtenidos exitosamente',
    'id_usuario' => $idEstudiante,
    'estudiante' => [
        'id_usuario' => (int)$estudiante['id_usuario'],
        'nombre' => $estudiante['nombre'],
        'correo' => $estudiante['correo']
    ],
    'total_logros' => count($logros),
    'logros_obtenidos' => (string)count($logros),
    'logros' => $logros
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$conn->close();
?>


