import { all, put, takeLatest, select } from 'redux-saga/effects';
import { setClusterState } from './RootActions';
import { GET_CLUSTER_STATE } from './RootActions';
import { api } from './ApiConfig';


function* getClusterState() {
    try {
        let response = yield api().get('/state');
        yield put(setClusterState(response.data))
    } catch (e) {
        console.log(e);
        //ignore
    }
}

export function* rootSaga() {
    yield all([
        yield takeLatest(GET_CLUSTER_STATE, getClusterState)
    ]);
}