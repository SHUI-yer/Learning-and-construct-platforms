[CmdletBinding()]
param(
    [string]$BaseRef
)

$ErrorActionPreference = 'Stop'

$allowedProductionConfig = 'light-out/lp-starter/src/main/resources/application-prod.yaml'

if ($BaseRef) {
    git cat-file -e "$BaseRef`^{commit}" 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "Base commit does not exist locally: $BaseRef"
    }
    $diffRange = "$BaseRef...HEAD"
    $stagedFiles = @(git diff $diffRange --name-only --diff-filter=ACMR)
} else {
    $stagedFiles = @(git diff --cached --name-only --diff-filter=ACMR)
}
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to inspect changed files.'
}

$forbidden = $stagedFiles | Where-Object {
    ($_ -match '(^|/)(\.env|\.env\.(?!example$).+|application-(local|secret)\.(yml|yaml|properties))$') -or
    ($_ -match '(^|/)application-prod\.(yml|yaml|properties)$' -and $_ -ne $allowedProductionConfig) -or
    $_ -match '\.(bak|pem|key|p12|jks|keystore)$'
}

if ($forbidden) {
    Write-Error "Sensitive or local files are staged:`n$($forbidden -join "`n")"
    exit 1
}

if ($BaseRef) {
    $diff = @(git diff $diffRange --no-ext-diff --unified=0 -- . ':!scripts/check-sensitive.ps1')
} else {
    $diff = @(git diff --cached --no-ext-diff --unified=0 -- . ':!scripts/check-sensitive.ps1')
}
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to inspect staged changes.'
}

$matches = foreach ($line in $diff) {
    if ($line -notmatch '^\+') { continue }
    if ($line -match '(?i)^\+.*(?:password|secret|token|api[_-]?key)\s*[:=]\s*(?!\$\{|replace-me|change-me|your_|example|<)[^\s#]+') {
        $line
    }
}

if ($matches) {
    Write-Error "Possible hard-coded credentials found in staged changes:`n$($matches -join "`n")"
    exit 1
}

Write-Output 'Sensitive-file and changed-credential checks passed.'
