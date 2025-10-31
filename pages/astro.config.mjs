// @ts-check
import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightImageZoomPlugin from "starlight-image-zoom";
import starlightScrollToTop from 'starlight-scroll-to-top';


// https://astro.build/config
export default defineConfig({
    site: 'https://smartboot.tech/',
    base: '/redisun',
    trailingSlash: "always",
    integrations: [
        starlight({
            title: 'Redisun',
            logo: {
                src: './src/assets/icon.svg',
            },
            customCss: [
                // 你的自定义 CSS 文件的相对路径
                './src/styles/custom.css',
            ],
            head: [
                {
                    tag: 'meta',
                    attrs: {
                        property: 'keywords',
                        content: 'redisun,redis,java,redis client,java redis,in-memory cache,key-value store',
                    }
                }, {
                    tag: 'meta',
                    attrs: {
                        property: 'description',
                        content: 'Redisun是一个基于Java语言开发的高性能Redis客户端，提供与Redis服务器兼容的内存键值存储访问',
                    }
                },
                // {
                //     tag: 'script',
                //     attrs: {
                //         src: 'https://smartboot.tech/js/gitee.js'
                //     }
                // },{
                //     tag:'script',
                //     content: `if(!location.pathname.endsWith("redisun/")&&!location.pathname.endsWith("/unstar/")&&!location.pathname.endsWith("/auth/")){
                //                 checkStar("smartboot","redisun",function(){
                //                     location.href="/redisun/unstar/";
                //                 });
                //             }`
                // },
                {
                    tag: 'script',
                    content: `
                var _hmt = _hmt || [];
                (function() {
                  var hm = document.createElement("script");
                  hm.src = "https://hm.baidu.com/hm.js?ee8630857921d8030d612dbd7d751b55";
                  var s = document.getElementsByTagName("script")[0]; 
                  s.parentNode.insertBefore(hm, s);
                })();
          `
                }
            ],
            social: [
                {icon: 'github', label: 'GitHub', href: 'https://github.com/smartboot/redisun'},
                {icon: 'seti:git', label: 'Gitee', href: 'https://gitee.com/smartboot/redisun'}
            ],
            plugins: [starlightImageZoomPlugin(),starlightScrollToTop({
                // Button position
                // Tooltip text
                tooltipText: 'Back to top',
                showTooltip: true,
                // Use smooth scrolling
                // smoothScroll: true,
                // Visibility threshold (show after scrolling 20% down)
                threshold: 20,
                // Customize the SVG icon
                borderRadius: '50',
                // Show scroll progress ring
                showProgressRing: true,
                // Customize progress ring color
                progressRingColor: '#ff6b6b',
            })],
            // 为此网站设置英语为默认语言。
            defaultLocale: 'root',
            locales: {
                root: {
                    label: '简体中文',
                    lang: 'zh-CN',
                },
                // 英文文档在 `src/content/docs/en/` 中。
                'en': {
                    label: 'English',
                    lang: 'en'
                }
            },
            sidebar: [
                {
                    label: '关于',
                    translations: {
                        'en': 'About',
                    },
                    autogenerate: {directory: 'guides'},
                },
                {
                    label: 'Redis指令',
                    translations: {
                        'en': 'Commands',
                    },
                    items:[
                        {
                            label: '基础',
                            translations: {
                                'en': 'Generic',
                            },
                            autogenerate: {directory: 'cmd/generic'},
                        },
                        {
                            label: '字符串',
                            translations: {
                                'en': 'String',
                            },
                            autogenerate: {directory: 'cmd/string'},
                        },
                        {
                            label: '列表',
                            translations: {
                                'en': 'List',
                            },
                            autogenerate: {directory: 'cmd/list'},
                        },
                        {
                            label: '集合',
                            translations: {
                                'en': 'Set',
                            },
                            autogenerate: {directory: 'cmd/set'},
                        },
                        {
                            label: '有序集合',
                            translations: {
                                'en': 'Sorted Set',
                            },
                            autogenerate: {directory: 'cmd/sorted_set'},
                        },
                        {
                            label: '哈希',
                            translations: {
                                'en': 'Hash',
                            },
                            autogenerate: {directory: 'cmd/hash'},
                        },
                        {
                            label: '其他',
                            translations: {
                                'en': 'Other',
                            },
                            autogenerate: {directory: 'cmd/server'},
                        },
                    ],
                    // autogenerate: {directory: 'cmd'},
                },
            ],
        }),
    ],
});
