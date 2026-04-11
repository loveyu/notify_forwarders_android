#!/usr/bin/env python3
"""
验证多语言字符串资源的完整性
"""

import os
import xml.etree.ElementTree as ET
from pathlib import Path

def parse_strings_xml(file_path):
    """解析strings.xml文件，返回字符串键值对"""
    if not os.path.exists(file_path):
        return {}
    
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
        strings = {}
        
        for string_elem in root.findall('string'):
            name = string_elem.get('name')
            if name:
                strings[name] = string_elem.text or ''
        
        # 也解析string-array
        for array_elem in root.findall('string-array'):
            name = array_elem.get('name')
            if name:
                items = [item.text or '' for item in array_elem.findall('item')]
                strings[name] = f"Array with {len(items)} items"
        
        return strings
    except ET.ParseError as e:
        print(f"Error parsing {file_path}: {e}")
        return {}

def main():
    """主函数"""
    base_dir = Path("app/src/main/res")
    
    # 语言目录映射
    language_dirs = {
        'values': '简体中文 (默认)',
        'values-en': '英语',
        'values-zh-rTW': '繁体中文',
        'values-ja': '日语',
        'values-ru': '俄语',
        'values-fr': '法语',
        'values-de': '德语'
    }
    
    # 解析所有语言的字符串
    all_strings = {}
    for lang_dir, lang_name in language_dirs.items():
        strings_file = base_dir / lang_dir / "strings.xml"
        strings = parse_strings_xml(strings_file)
        all_strings[lang_dir] = {
            'name': lang_name,
            'strings': strings,
            'count': len(strings)
        }
        print(f"{lang_name}: {len(strings)} 个字符串")
    
    # 获取默认语言的字符串作为基准
    default_strings = all_strings['values']['strings']
    
    print(f"\n=== 字符串完整性检查 ===")
    
    # 检查每种语言是否包含所有默认语言的字符串
    missing_strings = {}
    for lang_dir, lang_data in all_strings.items():
        if lang_dir == 'values':  # 跳过默认语言
            continue
            
        lang_strings = lang_data['strings']
        missing = []
        
        for key in default_strings.keys():
            if key not in lang_strings:
                missing.append(key)
        
        if missing:
            missing_strings[lang_dir] = missing
            print(f"\n{lang_data['name']} 缺少 {len(missing)} 个字符串:")
            for key in missing[:5]:  # 只显示前5个
                print(f"  - {key}")
            if len(missing) > 5:
                print(f"  ... 还有 {len(missing) - 5} 个")
        else:
            print(f"\n{lang_data['name']}: ✅ 完整")
    
    # 检查是否有额外的字符串
    print(f"\n=== 额外字符串检查 ===")
    for lang_dir, lang_data in all_strings.items():
        if lang_dir == 'values':
            continue
            
        lang_strings = lang_data['strings']
        extra = []
        
        for key in lang_strings.keys():
            if key not in default_strings:
                extra.append(key)
        
        if extra:
            print(f"\n{lang_data['name']} 有 {len(extra)} 个额外字符串:")
            for key in extra[:3]:
                print(f"  + {key}")
            if len(extra) > 3:
                print(f"  ... 还有 {len(extra) - 3} 个")
    
    # 统计新增的多语言字符串
    new_multilang_strings = [
        'background_settings', 'background_settings_desc', 'battery_optimization_settings',
        'battery_optimization_failed', 'test_notification_title', 'test_notification_desc',
        'send_random_notification', 'send_progress_notification', 'test_notification_sent',
        'verification_code_prompt', 'verification_code', 'verification_code_hint',
        'verification_success', 'verification_failed', 'connect_and_verify', 'verify',
        'server_connection_failed', 'server_connection_error', 'server_address_required',
        'service_start_failed', 'battery_optimization_request_failed', 
        'battery_optimization_granted', 'battery_optimization_warning',
        'confirm_clear_title', 'confirm_clear_message', 'confirm_clear_button',
        'clear_notification_history', 'test_notification_channel', 
        'test_notification_channel_desc', 'progress_notification_channel',
        'progress_notification_channel_desc', 'progress_notification_test_title',
        'progress_notification_updating', 'progress_notification_current',
        'progress_notification_completed', 'foreground_service_channel',
        'foreground_service_channel_desc', 'foreground_service_title',
        'foreground_service_text', 'test_notification_prefix',
        'test_notification_titles', 'test_notification_contents',
        'server_verification_title', 'server_verification_desc',
        'cancel', 'confirm', 'current_language'
    ]
    
    print(f"\n=== 新增多语言字符串检查 ===")
    for lang_dir, lang_data in all_strings.items():
        lang_strings = lang_data['strings']
        found_count = sum(1 for key in new_multilang_strings if key in lang_strings)
        print(f"{lang_data['name']}: {found_count}/{len(new_multilang_strings)} 个新字符串")
    
    print(f"\n=== 总结 ===")
    print(f"支持语言数: {len(language_dirs)}")
    print(f"默认语言字符串数: {len(default_strings)}")
    print(f"新增多语言字符串数: {len(new_multilang_strings)}")
    
    if not missing_strings:
        print("✅ 所有语言的字符串都是完整的！")
    else:
        print(f"⚠️  有 {len(missing_strings)} 种语言缺少字符串")

if __name__ == "__main__":
    main()
