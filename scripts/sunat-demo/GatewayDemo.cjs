'use strict';
const http = require('node:http');
// Solo localhost. No fallback a ventas, productos o clientes comerciales.
const HOST = '127.0.0.1';
const PORT = 18080;
const allowedOrigins = new Set(['http://localhost:4200','http://127.0.0.1:4200']);
function route(method,p) {
  if(method==='POST' && /^\/(api\/)?auth\/login$/.test(p)) return [8080,'/api/auth/login'];
  if(method==='GET' && /^\/(api\/)?auth\/me$/.test(p)) return [8080,p];
  if(method==='GET' && p==='/api/productos') return [19084,'/ventas/demo/catalogo'];
  if(method==='GET' && p==='/api/productos/stock-bajo') return [19084,'/ventas/demo/catalogo/bajo'];
  if(method==='GET' && /^\/api\/productos\/\d+$/.test(p)) return [19084,p.replace('/api/productos','/ventas/demo/catalogo')];
  if(method==='GET' && p==='/api/clientes') return [19084,'/ventas/demo/clientes'];
  if(method==='GET' && /^\/api\/facturacion\/(dni\/\d{8}|ruc\/\d{11})$/.test(p)) return [8080,p];
  if(method==='GET' && /^\/api\/facturacion\/laboratorio\/comprobantes\/(B9B1|F9B1)\/\d{8}$/.test(p)) return [19085,p];
  if(method==='GET' && /^\/api\/ventas(?:\/historial|\/\d+(?:\/estado-sunat|\/comprobante-local)?)?$/.test(p)) return [19084,p.slice(4)];
  if(method==='POST' && p==='/api/ventas') return [19084,'/ventas'];
  if(method==='PUT' && /^\/api\/ventas\/\d+\/anular$/.test(p)) return [19084,p.slice(4)];
  return null;
}
function json(res,code,message){if(!res.headersSent){res.writeHead(code,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});res.end(JSON.stringify({status:code,message}));}else res.destroy();}
function server(){return http.createServer((req,res)=>{
  const origin=req.headers.origin;
  if(origin && !allowedOrigins.has(origin)){json(res,403,'Origen no autorizado');return;}
  if(origin){res.setHeader('Access-Control-Allow-Origin',origin);res.setHeader('Vary','Origin');res.setHeader('Access-Control-Allow-Headers','Authorization, Content-Type, Idempotency-Key');res.setHeader('Access-Control-Allow-Methods','GET, POST, PUT, OPTIONS');}
  if(req.method==='OPTIONS'){res.writeHead(204);res.end();return;}
  if(!req.url || !req.url.startsWith('/') || req.url.startsWith('//')){json(res,400,'Ruta invalida');return;}
  let u;try{u=new URL(req.url,'http://localhost');}catch{json(res,400,'Ruta invalida');return;}
  if(req.method==='GET'&&u.pathname==='/demo/health'){res.writeHead(200,{'Content-Type':'application/json'});res.end('{"gateway":"DEMO","validezFiscal":false}');return;}
  const target=route(req.method,u.pathname);
  if(!target){json(res,403,'Ruta no disponible en la demostracion. No se ha enviado a servicios comerciales.');return;}
  if(u.search.length>4096 || Number(req.headers['content-length']||0)>128*1024){json(res,413,'Solicitud demasiado grande');return;}
  let headers={};for(const k of ['authorization','content-type','content-length','idempotency-key','accept'])if(req.headers[k])headers[k]=req.headers[k];
  // Forward only explicitly allowed headers. Never forward an internal service token.
  const upstream=http.request({hostname:HOST,port:target[0],method:req.method,path:target[1]+u.search,headers,timeout:150000},r=>{
    const h={};for(const k of ['content-type','content-length','content-disposition','www-authenticate'])if(r.headers[k])h[k]=r.headers[k];
    res.writeHead(r.statusCode||502,{...h,'Cache-Control':'no-store'});r.pipe(res);
  });
  upstream.on('timeout',()=>upstream.destroy(new Error('timeout')));
  upstream.on('error',()=>json(res,503,'Servicio demo no disponible o resultado no confirmado. Revise historial antes de repetir.'));
  req.on('aborted',()=>upstream.destroy());
  let bytes=0;req.on('data',chunk=>{bytes+=chunk.length;if(bytes>128*1024){upstream.destroy();req.destroy();}});req.pipe(upstream);
});}
if(require.main===module){const s=server();s.listen(PORT,HOST,()=>console.log('Gateway DEMO local: http://127.0.0.1:'+PORT));s.on('error',e=>{console.error('Gateway DEMO: '+e.code);process.exitCode=1;});}
module.exports={route,server};
