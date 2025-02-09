import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import yargs from 'yargs';
import { hideBin } from 'yargs/helpers';
import * as iconPark from '@icon-park/svg';
import {setConfig} from '@icon-park/svg';

// 获取当前模块的路径
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 解析命令行参数
const argv = yargs(hideBin(process.argv))
    .option('output', {
        alias: 'o',
        type: 'string',
        description: 'Output directory for the icons',
        default: path.join(__dirname, 'icons'),
    })
    .option('theme', {
        alias: 't',
        type: 'string',
        description: 'Icon theme (outline, filled, two-tone, etc.)',
        default: 'outline',
    })
    .option('prefix', {
        alias: 'p',
        type: 'string',
        description: 'Prefix for the icon filenames',
        default: '',
    })
    .option('strokeWidth', {
        alias: 's',
        type: 'number',
        description: 'Stroke width of the icons',
        default: 4,
    })
    .help()
    .alias('help', 'h')
    .argv;

// 设置输出目录
const outputDir = path.resolve(argv.output);
if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
}

// 设置全局配置
setConfig({
    size: '48',
    strokeWidth: argv.strokeWidth,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    theme: 'outline',
    colors: {
        outline: {
            fill: '#000',
            background: 'transparent'
        },
    }
})

// 获取所有图标的名称
const iconNames = Object.keys(iconPark).filter((iconName) => {
    return typeof iconPark[iconName] === 'function' && iconName.charAt(0).match(/[A-Z]/);
});

console.log(`Exporting ${iconNames.length} icons to ${outputDir}...`);

// 遍历所有图标并导出
iconNames.forEach((iconName) => {
    // 获取图标生成函数
    const iconFunction = iconPark[iconName];

    // 生成 SVG 内容
    const svgContent = iconFunction({ theme: argv.theme });

    // 生成文件名（支持前缀）
    const fileName = `${argv.prefix}${iconName}.svg`;
    const filePath = path.join(outputDir, fileName);

    // 将 SVG 内容写入文件
    fs.writeFileSync(filePath, svgContent);

    console.log(`Exported ${fileName} to ${filePath}`);
});

console.log(`All icons have been exported successfully to ${outputDir}!`);