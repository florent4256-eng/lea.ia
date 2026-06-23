import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.flolov42.lea_v3',
  appName: 'Léa',
  webDir: 'dist',
  server: {
    // 🚀 L'ARME LOURDE : On pointe vers ton domaine Cloudflare 100% sécurisé (HTTPS)
    url: 'https://lea-bunker.lea-ia-local.com',
    // 🛡️ ON BLINDE POUR LE MODE NATIF ET BIXBY FUTUR
    allowNavigation: ['lea-bunker.lea-ia-local.com'],
    hostname: 'lea-bunker.lea-ia-local.com'
    // ⚠️ J'ai retiré "cleartext: true" car nous sommes maintenant en HTTPS ultra-sécurisé !
  },
  android: {
    // 🤖 FORÇAGE DU MODE SYSTÈME POUR LES FUTURES PERMISSIONS
    allowMixedContent: true
  }
};

export default config;