<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

$payload = requireAuth();
$conn = getDBConnection();

ensureDefaultLogros($conn);

$data = json_decode(file_get_contents('php://input'), true);

// Aceptar múltiples nombres de parámetros
$idUsuario = isset($data['id_usuario']) ? (int)$data['id_usuario'] : (isset($data['usuario_id']) ? (int)$data['usuario_id'] : $payload['id_usuario']);
$idLogro = isset($data['id_logro']) ? (int)$data['id_logro'] : (isset($data['logro_id']) ? (int)$data['logro_id'] : null);
$fechaObtenido = isset($data['fecha_obtenido']) ? $data['fecha_obtenido'] : date('Y-m-d H:i:s');

// Validar parámetros requeridos
if ($idLogro === null) {
    http_response_code(400);
    echo jsonResponse(false, 'id_logro es requerido');
    $conn->close();
    exit();
}

// Verificar permisos: solo puede desbloquear su propio logro o ser docente/administrador
if ($idUsuario != $payload['id_usuario'] && !in_array($payload['rol'], ['docente', 'administrador'])) {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para desbloquear este logro');
    $conn->close();
    exit();
}

// Verificar que el usuario existe
$stmt = $conn->prepare("SELECT id_usuario FROM usuarios WHERE id_usuario = ?");
$stmt->bind_param("i", $idUsuario);
$stmt->execute();
$result = $stmt->get_result();
if ($result->num_rows === 0) {
    http_response_code(404);
    echo jsonResponse(false, 'Usuario no encontrado');
    $stmt->close();
    $conn->close();
    exit();
}
$stmt->close();

// Verificar que el logro existe
$stmt = $conn->prepare("SELECT id_logro FROM logros WHERE id_logro = ?");
$stmt->bind_param("i", $idLogro);
$stmt->execute();
$result = $stmt->get_result();
if ($result->num_rows === 0) {
    http_response_code(404);
    echo jsonResponse(false, 'Logro no encontrado');
    $stmt->close();
    $conn->close();
    exit();
}
$stmt->close();

// Verificar si el logro ya está desbloqueado
$stmt = $conn->prepare("SELECT id_usuario, id_logro, fecha_obtenido FROM usuario_logros WHERE id_usuario = ? AND id_logro = ?");
$stmt->bind_param("ii", $idUsuario, $idLogro);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    // Ya existe, actualizar fecha si es más reciente
    $existing = $result->fetch_assoc();
    $stmt->close();
    
    // Actualizar fecha si la nueva es más reciente o si no había fecha
    $stmt = $conn->prepare("UPDATE usuario_logros SET fecha_obtenido = ? WHERE id_usuario = ? AND id_logro = ?");
    $stmt->bind_param("sii", $fechaObtenido, $idUsuario, $idLogro);
    
    if (!$stmt->execute()) {
        http_response_code(500);
        echo jsonResponse(false, 'Error al actualizar logro desbloqueado');
        $stmt->close();
        $conn->close();
        exit();
    }
    
    $message = 'Logro actualizado exitosamente';
} else {
    // Insertar nuevo registro
    $stmt->close();
    $stmt = $conn->prepare("INSERT INTO usuario_logros (id_usuario, id_logro, fecha_obtenido) VALUES (?, ?, ?)");
    $stmt->bind_param("iis", $idUsuario, $idLogro, $fechaObtenido);
    
    if (!$stmt->execute()) {
        http_response_code(500);
        echo jsonResponse(false, 'Error al desbloquear logro: ' . $stmt->error);
        $stmt->close();
        $conn->close();
        exit();
    }
    
    $message = 'Logro desbloqueado exitosamente';
}

// Obtener el registro actualizado/insertado
$stmt->close();
$stmt = $conn->prepare("SELECT id_usuario, id_logro, fecha_obtenido FROM usuario_logros WHERE id_usuario = ? AND id_logro = ?");
$stmt->bind_param("ii", $idUsuario, $idLogro);
$stmt->execute();
$result = $stmt->get_result();
$row = $result->fetch_assoc();

// Obtener información del logro
$stmt->close();
$stmt = $conn->prepare("SELECT id_logro, titulo, descripcion FROM logros WHERE id_logro = ?");
$stmt->bind_param("i", $idLogro);
$stmt->execute();
$result = $stmt->get_result();
$logroInfo = $result->fetch_assoc();
$stmt->close();

$response = [
    'success' => true,
    'message' => $message,
    'data' => [
        'id_usuario' => (int)$row['id_usuario'],
        'id_logro' => (int)$row['id_logro'],
        'fecha_obtenido' => $row['fecha_obtenido'],
        'logro' => [
            'id_logro' => (int)$logroInfo['id_logro'],
            'titulo' => $logroInfo['titulo'],
            'descripcion' => $logroInfo['descripcion']
        ]
    ]
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$conn->close();
?>

