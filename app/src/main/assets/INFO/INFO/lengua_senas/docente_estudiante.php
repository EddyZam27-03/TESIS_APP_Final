<?php
require_once 'config.php';

$payload = requireAuth();
$conn = getDBConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $idUsuario = isset($_GET['id_usuario']) ? (int)$_GET['id_usuario'] : $payload['id_usuario'];
    $tipo = isset($_GET['tipo']) ? $_GET['tipo'] : null;
    
    if ($payload['rol'] !== 'administrador' && $idUsuario !== $payload['id_usuario']) {
        http_response_code(403);
        echo jsonResponse(false, 'No tiene permisos para ver estas relaciones');
        $conn->close();
        exit();
    }
    
    if ($tipo === 'docente') {
        $stmt = $conn->prepare("SELECT id_docente, id_estudiante, estado FROM docenteestudiante WHERE id_docente = ?");
    } elseif ($tipo === 'estudiante') {
        $stmt = $conn->prepare("SELECT id_docente, id_estudiante, estado FROM docenteestudiante WHERE id_estudiante = ?");
    } else {
        // Obtener todas las relaciones del usuario
        $stmt = $conn->prepare("SELECT id_docente, id_estudiante, estado FROM docenteestudiante WHERE id_docente = ? OR id_estudiante = ?");
        $stmt->bind_param("ii", $idUsuario, $idUsuario);
        $stmt->execute();
        $result = $stmt->get_result();
        $relaciones = [];
        while ($row = $result->fetch_assoc()) {
            $relaciones[] = [
                'id_docente' => (int)$row['id_docente'],
                'id_estudiante' => (int)$row['id_estudiante'],
                'estado' => $row['estado']
            ];
        }
        echo jsonResponse(true, 'Relaciones obtenidas exitosamente', $relaciones);
        $stmt->close();
        $conn->close();
        exit();
    }
    
    $stmt->bind_param("i", $idUsuario);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $relaciones = [];
    while ($row = $result->fetch_assoc()) {
        $relaciones[] = [
            'id_docente' => (int)$row['id_docente'],
            'id_estudiante' => (int)$row['id_estudiante'],
            'estado' => $row['estado']
        ];
    }
    
    echo jsonResponse(true, 'Relaciones obtenidas exitosamente', $relaciones);
    $stmt->close();
    
} elseif ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'), true);
    
    if (!isset($data['id_docente']) || !isset($data['id_estudiante'])) {
        http_response_code(400);
        echo jsonResponse(false, 'id_docente e id_estudiante son requeridos');
        $conn->close();
        exit();
    }
    
    $idDocente = (int)$data['id_docente'];
    $idEstudiante = (int)$data['id_estudiante'];
    $estado = isset($data['estado']) ? $conn->real_escape_string($data['estado']) : 'pendiente';
    
    // Verificar permisos
    if ($payload['rol'] !== 'administrador' && $idDocente != $payload['id_usuario']) {
        http_response_code(403);
        echo jsonResponse(false, 'No tiene permisos para crear esta relación');
        $conn->close();
        exit();
    }
    
    $stmt = $conn->prepare("INSERT INTO docenteestudiante (id_docente, id_estudiante, estado) VALUES (?, ?, ?)");
    $stmt->bind_param("iis", $idDocente, $idEstudiante, $estado);
    
    if ($stmt->execute()) {
        echo jsonResponse(true, 'Relación creada exitosamente', [
            'id_docente' => $idDocente,
            'id_estudiante' => $idEstudiante,
            'estado' => $estado
        ]);
    } else {
        http_response_code(500);
        echo jsonResponse(false, 'Error al crear relación');
    }
    
    $stmt->close();
    
} elseif ($_SERVER['REQUEST_METHOD'] === 'PUT') {
    $data = json_decode(file_get_contents('php://input'), true);
    
    if (!isset($data['id_docente']) || !isset($data['id_estudiante']) || !isset($data['estado'])) {
        http_response_code(400);
        echo jsonResponse(false, 'id_docente, id_estudiante y estado son requeridos');
        $conn->close();
        exit();
    }
    
    $idDocente = (int)$data['id_docente'];
    $idEstudiante = (int)$data['id_estudiante'];
    $estado = $conn->real_escape_string($data['estado']);
    
    // Verificar permisos: solo el docente asignado (o admin) puede aceptar/rechazar
    if (!in_array($payload['rol'], ['docente', 'administrador'])) {
        http_response_code(403);
        echo jsonResponse(false, 'No tiene permisos para actualizar esta relación');
        $conn->close();
        exit();
    }
    
    if ($payload['rol'] === 'docente' && $idDocente != $payload['id_usuario']) {
        http_response_code(403);
        echo jsonResponse(false, 'No tiene permisos para actualizar esta relación');
        $conn->close();
        exit();
    }
    
    $stmt = $conn->prepare("UPDATE docenteestudiante SET estado = ? WHERE id_docente = ? AND id_estudiante = ?");
    $stmt->bind_param("sii", $estado, $idDocente, $idEstudiante);
    
    if ($stmt->execute()) {
        echo jsonResponse(true, 'Relación actualizada exitosamente', [
            'id_docente' => $idDocente,
            'id_estudiante' => $idEstudiante,
            'estado' => $estado
        ]);
    } else {
        http_response_code(500);
        echo jsonResponse(false, 'Error al actualizar relación');
    }
    
    $stmt->close();
    
} elseif ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
    if (!isset($_GET['id_docente']) || !isset($_GET['id_estudiante'])) {
        http_response_code(400);
        echo jsonResponse(false, 'id_docente e id_estudiante son requeridos');
        $conn->close();
        exit();
    }
    
    $idDocente = (int)$_GET['id_docente'];
    $idEstudiante = (int)$_GET['id_estudiante'];
    
    // Solo administrador puede eliminar
    if ($payload['rol'] !== 'administrador') {
        http_response_code(403);
        echo jsonResponse(false, 'Solo administradores pueden eliminar relaciones');
        $conn->close();
        exit();
    }
    
    $stmt = $conn->prepare("DELETE FROM docenteestudiante WHERE id_docente = ? AND id_estudiante = ?");
    $stmt->bind_param("ii", $idDocente, $idEstudiante);
    
    if ($stmt->execute()) {
        echo jsonResponse(true, 'Relación eliminada exitosamente');
    } else {
        http_response_code(500);
        echo jsonResponse(false, 'Error al eliminar relación');
    }
    
    $stmt->close();
} else {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
}

$conn->close();
?>


