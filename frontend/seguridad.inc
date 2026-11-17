# Cabeceras de seguridad de los archivos del frontend (se incluye en cada location de nginx.conf).
# El navegador aplica a la vez esta cabecera y la CSP que el build pone en index.html (security.autoCsp en
# angular.json), y un script debe cumplir las dos. La del <meta> solo admite los scripts en línea con su hash
# exacto; por eso aquí script-src admite 'unsafe-inline': si no, bloquearía esos mismos scripts y la página
# no cargaría sus estilos. El resultado sigue siendo «solo scripts propios o con hash».
# Esta cabecera agrega lo que no se puede declarar en un <meta>, como frame-ancestors.
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header X-Frame-Options "DENY" always;
add_header Permissions-Policy "camera=(), microphone=(), geolocation=(), payment=()" always;
