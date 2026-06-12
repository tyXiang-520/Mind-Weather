/**
 * NJUmap.glb 压缩脚本
 * - Draco 压缩几何体
 * - 纹理压缩优化
 */

const { Document, NodeIO, Format } = require('@gltf-transform/core');
const { draco, textureCompress } = require('@gltf-transform/functions');
const {
  KHRDracoMeshCompression,
  KHRTextureTransform,
  KHRMaterialsEmissiveStrength,
  KHRMaterialsSpecular,
  KHRMaterialsIOR,
  KHRMaterialsUnlit,
  KHRMaterialsVolume,
  KHRMaterialsTransmission,
  KHRMaterialsSheen,
  KHRMaterialsClearcoat,
  EXTTextureWebP
} = require('@gltf-transform/extensions');
const draco3d = require('draco3d');
const fs = require('fs');

async function main() {
  console.log('📦 读取 NJUmap.glb...');
  const startTime = Date.now();

  // 初始化 Draco 编解码器
  console.log('🔧 初始化 Draco 编解码器...');
  const encoderModule = await draco3d.createEncoderModule({});
  const decoderModule = await draco3d.createDecoderModule({});

  const io = new NodeIO()
    .registerExtensions([
      KHRDracoMeshCompression,
      KHRTextureTransform,
      KHRMaterialsEmissiveStrength,
      KHRMaterialsSpecular,
      KHRMaterialsIOR,
      KHRMaterialsUnlit,
      KHRMaterialsVolume,
      KHRMaterialsTransmission,
      KHRMaterialsSheen,
      KHRMaterialsClearcoat,
      EXTTextureWebP
    ])
    .registerDependencies({
      'draco3d.encoder': encoderModule,
      'draco3d.decoder': decoderModule,
    });

  const buf = fs.readFileSync('NJUmap.glb');
  const arr = new Uint8Array(buf.buffer, buf.byteOffset, buf.byteLength);
  const document = await io.readBinary(arr);

  const root = document.getRoot();
  const origSizeMB = (buf.length / 1024 / 1024).toFixed(2);
  console.log(`  原始大小: ${origSizeMB} MB`);

  // ═══ 第一步: Draco 压缩 ═══
  console.log('🗜️  Draco 压缩几何体...');
  await document.transform(
    draco({
      method: 'edgebreaker',
      encodeSpeed: 3,              // 更慢=更好压缩率+更高质量
      decodeSpeed: 5,
      quantizePosition: 14,        // 位置精度提升（11→14，视觉无损）
      quantizeNormal: 10,          // 法线精度提升
      quantizeColor: 10,           // 颜色精度提升
      quantizeTexcoord: 12,        // UV 精度提升
      quantizeGeneric: 10,
    })
  );

  // ═══ 第二步: 纹理压缩（如果 sharp 可用） ═══
  console.log('🖼️  纹理压缩...');
  let sharpAvailable = false;
  try {
    require('sharp');
    sharpAvailable = true;
  } catch (e) {
    // sharp 未安装
  }

  if (sharpAvailable) {
    try {
      await document.transform(
        textureCompress({
          encoder: require('sharp'),
          targetFormat: 'webp',
          resize: [2048, 2048],
          quality: 85
        })
      );
      console.log('  纹理压缩完成');
    } catch (e) {
      console.log('  ⚠️ 纹理压缩跳过:', e.message);
    }
  } else {
    console.log('  ⚠️ sharp 未安装，跳过纹理压缩');
    console.log('   (npm install sharp 后重新运行以启用纹理压缩)');
  }

  // ═══ 写入 ═══
  console.log('💾 写入压缩后文件...');
  const compressed = await io.writeBinary(document);
  fs.writeFileSync('NJUmap_compressed.glb', Buffer.from(compressed.buffer));

  const compressedSizeMB = (compressed.length / 1024 / 1024).toFixed(2);
  const ratio = ((1 - compressed.length / buf.length) * 100).toFixed(1);
  const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);

  console.log('');
  console.log('✅ 压缩完成！');
  console.log(`  原始: ${origSizeMB} MB → 压缩: ${compressedSizeMB} MB (减小 ${ratio}%)`);
  console.log(`  耗时: ${elapsed}s`);
  console.log(`  输出: NJUmap_compressed.glb`);
}

main().catch(e => {
  console.error('❌ 压缩失败:', e);
  process.exit(1);
});
