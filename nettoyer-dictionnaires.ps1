$files = @(
    "src/main/resources/assets/olympicraft/dictionaries/fr_fr.txt",
    "src/main/resources/assets/olympicraft/dictionaries/en_us.txt"
)

foreach ($file in $files) {
    $lines = Get-Content $file -Encoding UTF8

    $filtered = $lines | Where-Object {
        $line = $_.Trim()

        # Conserve les commentaires et les lignes vides.
        if ($line.Length -eq 0 -or $line.StartsWith("#")) {
            return $true
        }

        # Conserve uniquement les mots d'au moins 4 caractères.
        return $line.Length -ge 5
    }

    $filtered |
        Set-Content $file -Encoding UTF8

    Write-Host "Dictionnaire nettoyé : $file" `
        -ForegroundColor Green
}