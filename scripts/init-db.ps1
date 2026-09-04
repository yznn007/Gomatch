param(
    [string]$EnvFile = ""
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$SqlFile = Join-Path $ProjectRoot "sql/init.sql"

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = if ([string]::IsNullOrWhiteSpace($env:GOMATCH_ENV_FILE)) {
        Join-Path $ProjectRoot ".env"
    } else {
        $env:GOMATCH_ENV_FILE
    }
}

if (-not (Test-Path -LiteralPath $EnvFile -PathType Leaf)) {
    throw "找不到 .env：$EnvFile。请先复制 .env.example 为 .env 并填写运行时配置。"
}
if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
    throw "未找到 mysql 命令，请先安装 MySQL 客户端。"
}

$DotEnv = @{}
Get-Content -LiteralPath $EnvFile | ForEach-Object {
    $Line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($Line) -or $Line.StartsWith("#")) {
        return
    }

    $Parts = $Line -split "=", 2
    if ($Parts.Count -ne 2) {
        throw "发现格式错误的 .env 配置：$Line"
    }

    $Key = $Parts[0].Trim()
    $Value = $Parts[1].Trim()
    if ($Value.Length -ge 2 -and (($Value.StartsWith('"') -and $Value.EndsWith('"')) -or ($Value.StartsWith("'") -and $Value.EndsWith("'")))) {
        $Value = $Value.Substring(1, $Value.Length - 2)
    }
    $DotEnv[$Key] = $Value
}

function Get-DotEnvValue([string]$Name) {
    if (-not $DotEnv.ContainsKey($Name)) {
        throw "缺少 .env 配置：$Name"
    }
    return [string]$DotEnv[$Name]
}

$DbUrl = Get-DotEnvValue "GOMATCH_DB_URL"
$DbUsername = Get-DotEnvValue "GOMATCH_DB_USERNAME"
$DbPassword = Get-DotEnvValue "GOMATCH_DB_PASSWORD"
$SeedPassword = Get-DotEnvValue "GOMATCH_SEED_PASSWORD"

if ($DbUrl -notmatch '^jdbc:mysql://(?<host>[^/:]+)(:(?<port>[0-9]+))?/(?<database>[^?]+)') {
    throw "GOMATCH_DB_URL 必须使用 jdbc:mysql://host[:port]/database 格式。"
}
$DbHost = $Matches.host
$DbPort = if ($Matches.port) { $Matches.port } else { "3306" }
$DbName = $Matches.database

if ($DbName -notmatch '^[A-Za-z0-9_]+$') {
    throw "GOMATCH_DB_URL 中的数据库名无效。"
}
if ($DbUsername -notmatch '^[A-Za-z0-9_]+$') {
    throw "GOMATCH_DB_USERNAME 只能包含字母、数字和下划线。"
}
if ([string]::IsNullOrWhiteSpace($DbPassword)) {
    throw "GOMATCH_DB_PASSWORD 不能为空。"
}
if ([string]::IsNullOrWhiteSpace($SeedPassword)) {
    throw "GOMATCH_SEED_PASSWORD 不能为空。"
}

function ConvertTo-SqlLiteral([string]$Value) {
    return $Value.Replace('\', '\\').Replace("'", "''")
}

$EscapedPassword = ConvertTo-SqlLiteral $DbPassword
$EscapedSeedPassword = ConvertTo-SqlLiteral $SeedPassword
$InitSql = Get-Content -LiteralPath $SqlFile -Raw
$InitSql = $InitSql.Replace('${GOMATCH_SEED_PASSWORD}', $EscapedSeedPassword)
$AccountSql = @"

CREATE USER IF NOT EXISTS '$DbUsername'@'localhost' IDENTIFIED BY '$EscapedPassword';
ALTER USER '$DbUsername'@'localhost' IDENTIFIED BY '$EscapedPassword';
CREATE USER IF NOT EXISTS '$DbUsername'@'127.0.0.1' IDENTIFIED BY '$EscapedPassword';
ALTER USER '$DbUsername'@'127.0.0.1' IDENTIFIED BY '$EscapedPassword';
GRANT SELECT, INSERT, UPDATE, DELETE ON ``$DbName``.* TO '$DbUsername'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON ``$DbName``.* TO '$DbUsername'@'127.0.0.1';
FLUSH PRIVILEGES;
"@

Write-Host "[INFO] 将初始化数据库 $DbName，并配置本地应用账号 $DbUsername。"
Write-Host "[INFO] MySQL 客户端将提示输入 root 密码；应用密码只从 .env 读取。"

($InitSql + [Environment]::NewLine + $AccountSql) | & mysql --protocol=TCP --host=$DbHost --port=$DbPort --user=root -p --default-character-set=utf8mb4
if ($LASTEXITCODE -ne 0) {
    throw "数据库初始化失败，mysql 退出码：$LASTEXITCODE"
}

Write-Host "[OK] 数据库初始化完成。"
