import React from 'react';

import { createStyles, Theme, makeStyles } from '@material-ui/core/styles';

import Typography from '@material-ui/core/Typography';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Paper from '@material-ui/core/Paper';

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        root: {
            width: '100%',
            maxWidth: 360,
            backgroundColor: theme.palette.background.paper,
        },
        nested: {
            paddingLeft: theme.spacing(4),
        },
        table: {
        },
        text: {
            padding: '16px'
        }
    }),
);

export default function Log(props: any) {
    const { log } = props;
    const classes = useStyles();

    return (<>
        <Typography
            variant="subtitle1"
            gutterBottom
            className={classes.text}>State machine: {log.stateMachineValue}</Typography>
        <TableContainer component={Paper}>
            <Table className={classes.table} aria-label="simple table">
                <TableHead>
                    <TableRow>
                        <TableCell>Index</TableCell>
                        <TableCell align="center">Leader term</TableCell>
                        <TableCell align="center">Value</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {log.log.map((value: any, i: number) => (
                        <TableRow key={i}>
                            <TableCell component="th" scope="row">{i}</TableCell>
                            <TableCell align="center">{value.leaderTerm}</TableCell>
                            <TableCell align="center">{value.value}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    </>);
}