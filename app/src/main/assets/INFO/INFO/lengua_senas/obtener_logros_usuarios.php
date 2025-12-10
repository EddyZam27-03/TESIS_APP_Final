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
$idUsuario = isset($_GET['id_usuario']) ? (int)$_GET['id_usuario'] : (isset($_GET['usuario_id']) ? (int)$_GET['usuario_id'] : null);
$idAdmin = isset($_GET['id_admin']) ? (int)$_GET['id_admin'] : null;
$idEstudiante = isset($_GET['id_estudiante']) ? (int)$_GET['id_estudiante'] : null;

// Determinar el usuario objetivo
$targetUsuarioId = $idEstudiante ?: $idUsuario ?: $payload['id_usuario'];

// Verificar permisos
if ($targetUsuarioId != $payload['id_usuario'] && !in_array($payload['rol'], ['docente', 'administrador'])) {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para ver estos logros');
    $conn->close();
    exit();
}

// Obtener información del usuario
$stmt = $conn->prepare("SELECT id_usuario, nombre, correo FROM usuarios WHERE id_usuario = ?");
$stmt->bind_param("i", $targetUsuarioId);
$stmt->execute();
$result = $stmt->get_result();
$usuario = $result->fetch_assoc();
$stmt->close();

if (!$usuario) {
    http_response_code(404);
    echo jsonResponse(false, 'Usuario no encontrado');
    $conn->close();
    exit();
}

// Obtener logros registrados para el usuario y devolver lista (coincide con ApiService)
$stmt = $conn->prepare("
    SELECT l.id_logro, l.titulo, l.descripcion, ul.fecha_obtenido
    FROM usuario_logros ul
    JOIN logros l ON l.id_logro = ul.id_logro
    WHERE ul.id_usuario = ?
    ORDER BY ul.fecha_obtenido DESC
");
$stmt->bind_param("i", $targetUsuarioId);
$stmt->execute();
$result = $stmt->get_result();

$logros = [];
while ($row = $result->fetch_assoc()) {
    $logros[] = [
        'id_usuario' => (int)$usuario['id_usuario'],
        'id_logro' => (int)$row['id_logro'],
        'titulo' => $row['titulo'],
        'descripcion' => $row['descripcion'],
        'fecha_obtenido' => $row['fecha_obtenido'],
        'nombre' => $usuario['nombre'],
        'correo' => $usuario['correo'],
        'desbloqueado' => true
    ];
}
$stmt->close();

echo json_encode($logros, JSON_UNESCAPED_UNICODE);

$conn->close();
?>


