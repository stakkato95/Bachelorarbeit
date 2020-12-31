import { all, put, takeLatest, select } from 'redux-saga/effects';
import { setClusterState, updateValueToReplicate } from './RootActions';
import { GET_CLUSTER_STATE, REPLICATE_VALUE } from './RootActions';
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

function* replicateValue() {
    const state = yield select();

    try {
        
        let clusterItem = { value: state.valueToReplicate }
        console.log(clusterItem)
        yield api().post('/item', clusterItem);
        yield put(updateValueToReplicate(""))
    } catch (e) {
        console.log(e);
        //ignore
    }
}

export function* rootSaga() {
    yield all([
        yield takeLatest(GET_CLUSTER_STATE, getClusterState),
        yield takeLatest(REPLICATE_VALUE, replicateValue)
    ]);
}