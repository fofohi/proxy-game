# proxy-game
test
/**
                             *     test4 out find pre so test4 is first then find pre is default
                             *     then skip to test3 find it's pre is test4 then invoke test4
                             *
                             *     default find default's next test1, then run test1,
                             *      ` then find test1's next test2,then run test2
                             *
                             *     outbound when add first a then b , run as a then b
                             *              when add last a then b , run as b then a
                             *     inbound when add first a then b , run as b then a
                             *             when add last a then b , run as a then b
                             *     if use fits logic
                             *     use inbound add last
                             *     use outbound add first as wish
                             *
                             *     outbound is find pre
                             *     inbound is find next
                             *
                             *
                             *      server 负责进行request 解码and response编码
                             *      HttpServerCodec 里面组合了HttpResponseEncoder和HttpRequestDecoder
                             *
                             *      cline 负责进行request变码 and response 解码
                             *      HttpClientCodec 里面组合了HttpRequestEncoder和HttpResponseDecoder
                             *
                             *
                             *
                             *
                             */
