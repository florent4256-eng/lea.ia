import { Capacitor } from '@capacitor/core';

async function blobToBase64(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => resolve((reader.result as string).split(',')[1]);
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}

// Partager via le menu de partage Android
export async function downloadFile(url: string, filename: string): Promise<void> {
  if (Capacitor.isNativePlatform()) {
    const { Filesystem, Directory } = await import('@capacitor/filesystem');
    const { Share } = await import('@capacitor/share');
    const resp = await fetch(url);
    if (!resp.ok) throw new Error('Fichier inaccessible');
    const blob = await resp.blob();
    const b64 = await blobToBase64(blob);
    const saved = await Filesystem.writeFile({
      path: filename,
      data: b64,
      directory: Directory.Cache,
    });
    await Share.share({ title: filename, url: saved.uri, dialogTitle: 'Partager' });
  } else {
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }
}

// Sauvegarder directement dans Download/Léa/ sur Android
// onFeedback optionnel : permet à l'appelant d'afficher son propre toast au lieu
// du alert() natif du navigateur. Sans callback, alert() reste le filet de sécurité
// (comportement inchangé pour les appelants qui n'ont pas encore été mis à jour).
export async function saveFile(url: string, filename: string, onFeedback?: (msg: string) => void): Promise<void> {
  const notify = onFeedback || alert;
  if (Capacitor.isNativePlatform()) {
    try {
      const resp = await fetch(url);
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
      const blob = await resp.blob();
      const b64 = await blobToBase64(blob);

      const { Filesystem, Directory } = await import('@capacitor/filesystem');
      await Filesystem.writeFile({
        path: `Download/Léa/${filename}`,
        data: b64,
        directory: Directory.ExternalStorage,
        recursive: true,
      });
      notify(`✅ Image sauvegardée dans Download/Léa/`);
    } catch (err: any) {
      // Si ExternalStorage échoue (permission), on bascule sur le partage
      try {
        await downloadFile(url, filename);
      } catch {
        notify(`❌ Impossible de sauvegarder : ${err?.message || err}`);
      }
    }
  } else {
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }
}
