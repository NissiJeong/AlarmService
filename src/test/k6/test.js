import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    vus: 1,             // 가상 유저 수
    iterations: '5',     // 1번만 실행
};

export default function () {
    let res = http.post('http://localhost:8080/products/1/notifications/re-stock'); // 스프링 엔드포인트 주소
    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 1000ms': (r) => r.timings.duration < 1000,
    });
    sleep(1); // 1초 대기
}