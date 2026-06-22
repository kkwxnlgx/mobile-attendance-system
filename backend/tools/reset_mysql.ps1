$ErrorActionPreference = 'Continue'
$log = 'C:\Users\LENOVO\Desktop\as\finalwork\backend\tools\reset_log.txt'
Start-Transcript -Path $log -Force
try {
    Write-Output 'Stopping MySQL84 service...'
    Stop-Service MySQL84 -Force
    (Get-Service MySQL84).WaitForStatus('Stopped', '00:00:30')
    Write-Output "Service status: $((Get-Service MySQL84).Status)"

    $mysqld = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe'
    $initFile = 'C:\Users\LENOVO\Desktop\as\finalwork\backend\tools\reset.sql'
    Write-Output 'Starting mysqld with --init-file...'
    $proc = Start-Process -FilePath $mysqld -ArgumentList "--init-file=$initFile" -PassThru -WindowStyle Hidden

    $ok = $false
    foreach ($i in 1..30) {
        Start-Sleep -Seconds 2
        $tcp = New-Object Net.Sockets.TcpClient
        try { $tcp.Connect('127.0.0.1', 3306); $ok = $tcp.Connected } catch {} finally { $tcp.Close() }
        if ($ok) { break }
    }
    Write-Output "Port 3306 up: $ok"
    Start-Sleep -Seconds 3

    $mysqladmin = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqladmin.exe'
    Write-Output 'Shutting down temporary mysqld...'
    & $mysqladmin -u root -pattend123 shutdown 2>&1 | Out-String | Write-Output
    Write-Output "mysqladmin exit code: $LASTEXITCODE"

    if (-not $proc.WaitForExit(30000)) {
        Write-Output 'mysqld did not exit, killing...'
        Stop-Process -Id $proc.Id -Force
        Start-Sleep -Seconds 3
    }

    Write-Output 'Restarting MySQL84 service...'
    Start-Service MySQL84
    (Get-Service MySQL84).WaitForStatus('Running', '00:00:30')
    Write-Output "Final service status: $((Get-Service MySQL84).Status)"
    Write-Output 'DONE'
} finally {
    Stop-Transcript
}
