import axios from 'axios';

var axiosInstance = axios.create({
    baseURL: 'http://localhost:9000/cluster'
});

export function api() {
    return axiosInstance;
}